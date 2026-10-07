package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.domain.ConflictResolution;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Posts one staged record through {@link TransactionService}, each call in its own transaction.
 *
 * <p>A regular record becomes a transaction with two journals: its account against the account of its category. A
 * transfer between own accounts becomes one transaction with the journals of both legs. A positive amount is a debit,
 * a negative one a credit. A transaction changed in FRP since it was posted is never overwritten or deleted; the record
 * becomes {@link ImportRecordStatus#CONFLICT} instead.
 */
@Service
@RequiredArgsConstructor
public class RecordPostingService {

    private static final int DESCRIPTION_LENGTH = 255;
    private static final int FX_RATE_SCALE = 8;
    private static final String CHANGED_IN_FRP = "Transaction was changed in FRP";
    private static final String DELETED_IN_FRP = "Transaction was deleted in FRP";

    private final AccImportRecordRepository importRecordRepository;
    private final RecordMappingService recordMappingService;
    private final TransactionService transactionService;
    private final CurrencyService currencyService;

    /**
     * Posts a {@link ImportRecordStatus#NEW} record as a new transaction, or rewrites the journals of the transaction
     * it was posted as before. A transfer waits until its other leg is staged and mapped.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostingOutcome post(MappedRecord mapped) {
        var importRecord = findRecord(mapped.importRecordId());
        if (importRecord.getStatus() != ImportRecordStatus.NEW) {
            return PostingOutcome.UNCHANGED;
        }
        Optional<List<Leg>> legs = mapped.counterAccountId() == null
                ? transferLegs(importRecord, mapped.accountId())
                : Optional.of(regularLegs(importRecord, mapped));
        return legs.map(this::post).orElse(PostingOutcome.WAITING);
    }

    /**
     * Deletes the transaction of a record deleted in the source. The other leg of a transfer loses the transaction
     * too and waits to be posted again.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostingOutcome removeTransaction(Long importRecordId) {
        var importRecord = findRecord(importRecordId);
        Long transactionId = importRecord.getTransactionId();
        if (transactionId == null) {
            return PostingOutcome.UNCHANGED;
        }
        if (isChangedInFrp(transactionId, importRecord.getPostedHash())) {
            markConflict(importRecord, CHANGED_IN_FRP);
            return PostingOutcome.CONFLICT;
        }
        for (var linked : importRecordRepository.findByTransactionId(transactionId)) {
            linked.setTransactionId(null);
            linked.setPostedHash(null);
            if (linked.getStatus() != ImportRecordStatus.DELETED) {
                linked.setStatus(ImportRecordStatus.NEW);
            }
        }
        transactionService.deleteTransaction(transactionId);
        return PostingOutcome.REMOVED;
    }

    /**
     * Notes why posting the record failed. A pending record becomes {@link ImportRecordStatus#ERROR}; a deleted one
     * stays deleted, so the removal of its transaction is retried.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long importRecordId, String errorMessage) {
        var importRecord = findRecord(importRecordId);
        if (importRecord.getStatus() != ImportRecordStatus.DELETED) {
            importRecord.setStatus(ImportRecordStatus.ERROR);
        }
        importRecord.setErrorMessage(errorMessage);
    }

    /**
     * Resolves the conflict of the record together with the other leg of its transfer.
     * {@link ConflictResolution#KEEP_FRP} accepts the transaction as it is in FRP now, or its deletion, as the posted
     * state of the record. {@link ConflictResolution#USE_SOURCE} lets the next posting overwrite, recreate or (for a
     * record deleted in the source) delete the transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveConflict(Long importRecordId, ConflictResolution resolution) {
        var importRecord = findRecord(importRecordId);
        var currentHash = Optional.ofNullable(importRecord.getTransactionId())
                .map(transactionId -> postedHash(transactionService.getTransaction(transactionId)))
                .orElse(null);
        Stream.concat(Stream.of(importRecord), otherLegsOf(importRecord))
                .filter(conflicting -> conflicting.getStatus() == ImportRecordStatus.CONFLICT)
                .forEach(conflicting -> resolve(conflicting, resolution == ConflictResolution.KEEP_FRP, currentHash));
    }

    /**
     * Fingerprint of the content of a transaction that a user can change: description, reference, rate and journals.
     */
    static String postedHash(AccTransactionDto transaction) {
        var journals = transaction.journals().stream()
                .map(journal -> ContentHash.join(Stream.of(journal.date(), journal.description(), journal.accountId(),
                        journal.credit(), journal.debit())))
                .sorted();
        return ContentHash.of(Stream.concat(
                Stream.of(transaction.reference(), transaction.description(), transaction.fxRate()), journals));
    }

    private PostingOutcome post(List<Leg> legs) {
        var records = legs.stream().map(Leg::importRecord).distinct().toList();
        var posted = records.stream().filter(importRecord -> importRecord.getPostedHash() != null).findFirst();
        var conflict = posted.flatMap(this::conflictOf);
        if (conflict.isPresent()) {
            records.stream().filter(importRecord -> importRecord.getStatus() == ImportRecordStatus.NEW)
                    .forEach(importRecord -> markConflict(importRecord, conflict.get()));
            return PostingOutcome.CONFLICT;
        }
        validate(legs);
        var request = requestOf(legs);
        var source = legs.getFirst().importRecord();
        var transaction = posted.map(AccImportRecordEntity::getTransactionId)
                .map(transactionId -> transactionService.updateTransaction(transactionId, request))
                .orElseGet(() -> transactionService.createImportedTransaction(request, source.getConnectionId(),
                        source.getExternalId()));
        var hash = postedHash(transaction);
        records.forEach(importRecord -> markPosted(importRecord, transaction.id(), hash));
        return PostingOutcome.POSTED;
    }

    private Optional<String> conflictOf(AccImportRecordEntity posted) {
        if (posted.getTransactionId() == null) {
            return Optional.of(DELETED_IN_FRP);
        }
        return isChangedInFrp(posted.getTransactionId(), posted.getPostedHash())
                ? Optional.of(CHANGED_IN_FRP) : Optional.empty();
    }

    private boolean isChangedInFrp(Long transactionId, String postedHash) {
        return !postedHash(transactionService.getTransaction(transactionId)).equals(postedHash);
    }

    private static List<Leg> regularLegs(AccImportRecordEntity importRecord, MappedRecord mapped) {
        return sorted(Stream.of(new Leg(importRecord, mapped.accountId(), importRecord.getAmount()),
                new Leg(importRecord, mapped.counterAccountId(), importRecord.getAmount().negate())));
    }

    /**
     * Legs of a transfer, or empty while the other leg is not staged or its account is not mapped.
     */
    private Optional<List<Leg>> transferLegs(AccImportRecordEntity importRecord, Long accountId) {
        var connectionId = importRecord.getConnectionId();
        return importRecordRepository.findByConnectionIdAndTransferLinkIdAndIdNot(connectionId,
                        importRecord.getTransferLinkId(), importRecord.getId()).stream()
                .filter(other -> other.getStatus() != ImportRecordStatus.DELETED)
                .findFirst()
                .flatMap(other -> recordMappingService.mappedAccountOf(connectionId, other.getExternalAccountId())
                        .map(otherAccountId -> sorted(Stream.of(
                                new Leg(importRecord, accountId, importRecord.getAmount()),
                                new Leg(other, otherAccountId, other.getAmount())))));
    }

    /**
     * Outgoing leg first; it is the source the transaction is linked to.
     */
    private static List<Leg> sorted(Stream<Leg> legs) {
        return legs.sorted(Comparator.comparing(Leg::amount)).toList();
    }

    private static void validate(List<Leg> legs) {
        var first = legs.getFirst().importRecord();
        if (legs.stream().anyMatch(leg -> leg.amount().signum() == 0)) {
            throw new IllegalStateException("Record " + first.getExternalId() + " has a zero amount");
        }
        var currencies = legs.stream().map(leg -> leg.importRecord().getCurrencyCode()).distinct().toList();
        if (currencies.size() > 1) {
            throw new IllegalStateException("Transfer between currencies " + String.join(" and ", currencies)
                    + " is not supported");
        }
        var balance = legs.stream().map(Leg::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (balance.signum() != 0) {
            throw new IllegalStateException("Amounts of transfer " + first.getTransferLinkId() + " do not match");
        }
    }

    private AccTransactionCreateRequestDto requestOf(List<Leg> legs) {
        var source = legs.getFirst().importRecord();
        var journals = legs.stream()
                .map(leg -> new AccJournalCreateRequestDto(leg.importRecord().getRecordDate(),
                        descriptionOf(leg.importRecord()), leg.accountId(), leg.amount().negate().max(BigDecimal.ZERO),
                        leg.amount().max(BigDecimal.ZERO)))
                .toList();
        return new AccTransactionCreateRequestDto(null, descriptionOf(source), fxRateOf(source), journals);
    }

    /**
     * Rate to the base currency from the amount the source converted; {@code null} for a record in the base currency
     * or when the source did not convert it to the base currency.
     */
    private BigDecimal fxRateOf(AccImportRecordEntity importRecord) {
        var baseCurrencyCode = currencyService.getBaseCurrencyCode();
        if (baseCurrencyCode.equals(importRecord.getCurrencyCode()) || importRecord.getBaseAmount() == null
                || !baseCurrencyCode.equals(importRecord.getBaseCurrencyCode())) {
            return null;
        }
        return importRecord.getBaseAmount().abs()
                .divide(importRecord.getAmount().abs(), FX_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static String descriptionOf(AccImportRecordEntity importRecord) {
        var description = Stream.of(importRecord.getCounterparty(), importRecord.getNote())
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining(" - "));
        if (description.isEmpty()) {
            return null;
        }
        return description.length() > DESCRIPTION_LENGTH ? description.substring(0, DESCRIPTION_LENGTH) : description;
    }

    private static void markPosted(AccImportRecordEntity importRecord, Long transactionId, String postedHash) {
        importRecord.setTransactionId(transactionId);
        importRecord.setPostedHash(postedHash);
        importRecord.setStatus(ImportRecordStatus.POSTED);
        importRecord.setErrorMessage(null);
    }

    private Stream<AccImportRecordEntity> otherLegsOf(AccImportRecordEntity importRecord) {
        if (importRecord.getTransferLinkId() == null) {
            return Stream.empty();
        }
        return importRecordRepository.findByConnectionIdAndTransferLinkIdAndIdNot(importRecord.getConnectionId(),
                importRecord.getTransferLinkId(), importRecord.getId()).stream();
    }

    /**
     * A record deleted in the source either lets go of the kept transaction or stays deleted with the current
     * fingerprint, so the next posting deletes the transaction. Otherwise a kept transaction becomes the posted state
     * of the record and a used source is posted again; a transaction deleted in FRP is recreated only from the source.
     */
    private static void resolve(AccImportRecordEntity importRecord, boolean keepFrp, String currentHash) {
        importRecord.setErrorMessage(null);
        if (importRecord.getSourceState() == ExternalRecordState.DELETED) {
            importRecord.setStatus(ImportRecordStatus.DELETED);
            if (keepFrp) {
                importRecord.setTransactionId(null);
            }
            importRecord.setPostedHash(keepFrp ? null : currentHash);
        } else if (importRecord.getTransactionId() == null) {
            importRecord.setStatus(keepFrp ? ImportRecordStatus.SKIPPED : ImportRecordStatus.NEW);
            importRecord.setPostedHash(null);
        } else {
            importRecord.setStatus(keepFrp ? ImportRecordStatus.POSTED : ImportRecordStatus.NEW);
            importRecord.setPostedHash(currentHash);
        }
    }

    private static void markConflict(AccImportRecordEntity importRecord, String reason) {
        importRecord.setStatus(ImportRecordStatus.CONFLICT);
        importRecord.setErrorMessage(reason);
    }

    private AccImportRecordEntity findRecord(Long importRecordId) {
        return importRecordRepository.findById(importRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Import record not found: " + importRecordId));
    }

    /**
     * One journal of the transaction: the account and the signed amount of a record, positive = debit.
     */
    private record Leg(AccImportRecordEntity importRecord, Long accountId, BigDecimal amount) {
    }
}
