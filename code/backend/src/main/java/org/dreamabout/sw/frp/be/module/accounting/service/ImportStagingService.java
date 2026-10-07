package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAmount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecord;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Writes one page of a synchronization into the import staging (idempotent upsert by connection and external id)
 * and saves the progress of the run in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class ImportStagingService {

    private final AccImportRecordRepository importRecordRepository;
    private final AccConnectionRepository connectionRepository;
    private final Clock clock;

    /**
     * Upserts the records of the page. Unchanged records only get their last seen time; new and changed ones become
     * {@link ImportRecordStatus#NEW} (or {@link ImportRecordStatus#DELETED} when the source deleted them). After the
     * last page the records of the run's window that were not fetched are marked deleted and the run is finished;
     * otherwise the cursor of the next page is saved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportResult stagePage(Long connectionId, ImportRun run, RecordPage page) {
        var seenAt = clock.instant();
        var result = upsert(connectionId, page.records(), seenAt);
        int deleted = page.isLast() ? importRecordRepository.markNotSeenAsDeleted(connectionId, run.window().from(),
                run.window().to(), run.startedAt()) : 0;

        var connection = connectionRepository.findById(connectionId).orElseThrow();
        if (page.isLast()) {
            connection.setSyncState(run.finishedState());
            connection.setLastSuccessfulSyncAt(seenAt);
        } else {
            connection.setSyncState(run.withCursor(page.nextCursor()).toState());
        }
        connectionRepository.save(connection);
        return result.plus(new ImportResult(0, 0, 0, deleted));
    }

    /**
     * Finishes a synchronization that found the source data unchanged since the last one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishUnchanged(Long connectionId) {
        var connection = connectionRepository.findById(connectionId).orElseThrow();
        connection.setLastSuccessfulSyncAt(clock.instant());
        connectionRepository.save(connection);
    }

    private ImportResult upsert(Long connectionId, List<ExternalRecord> records, Instant seenAt) {
        Map<String, ExternalRecord> byExternalId = new LinkedHashMap<>();
        records.forEach(externalRecord -> byExternalId.put(externalRecord.externalId(), externalRecord));
        Map<String, AccImportRecordEntity> staged = new HashMap<>();
        importRecordRepository.findByConnectionIdAndExternalIdIn(connectionId, byExternalId.keySet())
                .forEach(stagedRecord -> staged.put(stagedRecord.getExternalId(), stagedRecord));

        List<AccImportRecordEntity> written = new ArrayList<>();
        List<Long> unchangedIds = new ArrayList<>();
        int created = 0;
        for (ExternalRecord externalRecord : byExternalId.values()) {
            var hash = payloadHash(externalRecord);
            var stagedRecord = staged.get(externalRecord.externalId());
            if (stagedRecord == null) {
                stagedRecord = newRecord(connectionId, externalRecord.externalId(), seenAt);
                created++;
            } else if (!isChanged(stagedRecord, externalRecord, hash)) {
                unchangedIds.add(stagedRecord.getId());
                continue;
            }
            apply(stagedRecord, externalRecord, hash, seenAt);
            written.add(stagedRecord);
        }
        importRecordRepository.saveAll(written);
        if (!unchangedIds.isEmpty()) {
            importRecordRepository.markSeen(unchangedIds, seenAt);
        }
        return new ImportResult(records.size(), created, written.size() - created, 0);
    }

    private static AccImportRecordEntity newRecord(Long connectionId, String externalId, Instant seenAt) {
        var stagedRecord = new AccImportRecordEntity();
        stagedRecord.setConnectionId(connectionId);
        stagedRecord.setExternalId(externalId);
        stagedRecord.setFirstSeenAt(seenAt);
        return stagedRecord;
    }

    private static boolean isChanged(AccImportRecordEntity stagedRecord, ExternalRecord source, String hash) {
        boolean reappeared = stagedRecord.getSourceState() == ExternalRecordState.DELETED
                && source.state() != ExternalRecordState.DELETED;
        return reappeared || !hash.equals(stagedRecord.getPayloadHash());
    }

    private static void apply(AccImportRecordEntity stagedRecord, ExternalRecord source, String hash, Instant seenAt) {
        stagedRecord.setExternalAccountId(source.externalAccountId());
        stagedRecord.setExternalCategoryId(source.externalCategoryId());
        stagedRecord.setRecordDate(source.date());
        stagedRecord.setAmount(source.amount().value());
        stagedRecord.setCurrencyCode(source.amount().currencyCode());
        var baseAmount = Optional.ofNullable(source.baseAmount());
        stagedRecord.setBaseAmount(baseAmount.map(ExternalAmount::value).orElse(null));
        stagedRecord.setBaseCurrencyCode(baseAmount.map(ExternalAmount::currencyCode).orElse(null));
        stagedRecord.setNote(source.note());
        stagedRecord.setCounterparty(source.counterparty());
        stagedRecord.setSourceState(source.state());
        stagedRecord.setTransferLinkId(source.transferLinkId());
        stagedRecord.setSourceUpdatedAt(source.updatedAt());
        stagedRecord.setRawPayload(source.rawPayload());
        stagedRecord.setPayloadHash(hash);
        stagedRecord.setStatus(source.state() == ExternalRecordState.DELETED
                ? ImportRecordStatus.DELETED : ImportRecordStatus.NEW);
        stagedRecord.setErrorMessage(null);
        stagedRecord.setLastSeenAt(seenAt);
    }

    /**
     * SHA-256 over the content of the record; {@code updatedAt} is left out so a mere touch in the source is no change.
     */
    static String payloadHash(ExternalRecord source) {
        var baseAmount = Optional.ofNullable(source.baseAmount());
        return ContentHash.of(Stream.of(source.externalAccountId(), source.date(), source.amount().value(),
                source.amount().currencyCode(), source.externalCategoryId(), source.note(), source.counterparty(),
                source.state(), source.transferLinkId(), source.rawPayload(),
                baseAmount.map(ExternalAmount::value).orElse(null),
                baseAmount.map(ExternalAmount::currencyCode).orElse(null)));
    }
}
