package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.domain.ConflictResolution;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDetailDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccMappedItemDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.ImportRecordMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccExternalMappingRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Review of the records staged from a connection: lists them by status, shows their detail and lets the user retry
 * failed records, ignore
 * records and resolve conflicts with changes made in FRP.
 *
 * <p>The actions hold the sync lock of the connection, so they fail while the connection is being synchronized.
 * Retrying and resolving post the pending records of the connection right away.
 */
@Service
@RequiredArgsConstructor
public class ImportRecordService {

    private static final Set<ImportRecordStatus> IGNORABLE = Set.of(ImportRecordStatus.NEW, ImportRecordStatus.ERROR);

    private final ConnectionService connectionService;
    private final ImportService importService;
    private final ImportPostingService postingService;
    private final RecordPostingService recordPostingService;
    private final AccImportRecordRepository importRecordRepository;
    private final ImportRecordMapper importRecordMapper;
    private final AccExternalMappingRepository mappingRepository;
    private final AccountService accountService;

    /**
     * Records of the connection in the statuses, the latest first. Records waiting for a mapping are
     * {@link ImportRecordStatus#NEW}.
     */
    @Transactional(readOnly = true)
    public List<AccImportRecordDto> getRecords(Long connectionId, Collection<ImportRecordStatus> statuses) {
        connectionService.findConnection(connectionId);
        return importRecordRepository.findByConnectionIdAndStatusInOrderByRecordDateDescIdDesc(connectionId, statuses)
                .stream()
                .map(importRecordMapper::toDto)
                .toList();
    }

    /**
     * Record of the connection with its mappings, the account of the other leg of a transfer and its representation
     * in the source.
     */
    @Transactional(readOnly = true)
    public AccImportRecordDetailDto getRecordDetail(Long connectionId, Long recordId) {
        var importRecord = importRecordRepository.findByIdAndConnectionId(recordId, connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Import record not found: " + recordId));
        var transferAccount = Optional.ofNullable(importRecord.getTransferLinkId())
                .flatMap(transferLinkId -> importRecordRepository.findByConnectionIdAndTransferLinkIdAndIdNot(
                        connectionId, transferLinkId, recordId).stream()
                        .filter(otherLeg -> otherLeg.getStatus() != ImportRecordStatus.DELETED)
                        .findFirst())
                .map(otherLeg -> mappedItem(connectionId, ExternalMappingKind.ACCOUNT, otherLeg.getExternalAccountId()))
                .orElse(null);
        return new AccImportRecordDetailDto(importRecordMapper.toDto(importRecord),
                mappedItem(connectionId, ExternalMappingKind.ACCOUNT, importRecord.getExternalAccountId()),
                mappedItem(connectionId, ExternalMappingKind.CATEGORY, importRecord.getExternalCategoryId()),
                transferAccount, importRecord.getBaseAmount(), importRecord.getBaseCurrencyCode(),
                importRecord.getSourceUpdatedAt(), importRecord.getFirstSeenAt(), importRecord.getRawPayload());
    }

    /**
     * Posts the {@link ImportRecordStatus#ERROR} record again together with the other pending records of the
     * connection.
     */
    @Transactional
    public AccImportRecordDto retry(Long connectionId, Long recordId) {
        importService.lockForSync(connectionId);
        requireStatus(connectionId, recordId, ImportRecordStatus.ERROR);
        postingService.post(connectionId);
        return getRecord(connectionId, recordId);
    }

    /**
     * Resolves the {@link ImportRecordStatus#CONFLICT} record (and the other leg of its transfer) and posts the
     * pending records of the connection.
     */
    @Transactional
    public AccImportRecordDto resolveConflict(Long connectionId, Long recordId, ConflictResolution resolution) {
        importService.lockForSync(connectionId);
        requireStatus(connectionId, recordId, ImportRecordStatus.CONFLICT);
        recordPostingService.resolveConflict(recordId, resolution);
        postingService.post(connectionId);
        return getRecord(connectionId, recordId);
    }

    /**
     * Skips a {@link ImportRecordStatus#NEW} or {@link ImportRecordStatus#ERROR} record; it is posted again only when
     * it changes in the source.
     */
    @Transactional
    public AccImportRecordDto ignore(Long connectionId, Long recordId) {
        importService.lockForSync(connectionId);
        var importRecord = importRecordRepository.findByIdAndConnectionId(recordId, connectionId)
                .filter(found -> IGNORABLE.contains(found.getStatus()))
                .orElseThrow(() -> new IllegalStateException(
                        "Import record " + recordId + " is not waiting to be posted in connection " + connectionId));
        importRecord.setStatus(ImportRecordStatus.SKIPPED);
        importRecord.setErrorMessage(null);
        return importRecordMapper.toDto(importRecordRepository.save(importRecord));
    }

    /**
     * Checks the record without loading it into the persistence context of the caller, which then reads the record
     * as the posting committed it.
     */
    private void requireStatus(Long connectionId, Long recordId, ImportRecordStatus status) {
        if (!importRecordRepository.existsByIdAndConnectionIdAndStatus(recordId, connectionId, status)) {
            throw new IllegalStateException(
                    "Import record " + recordId + " is not in status " + status + " in connection " + connectionId);
        }
    }

    /**
     * External account or category with its mapping; {@code null} for a missing external id.
     */
    private AccMappedItemDto mappedItem(Long connectionId, ExternalMappingKind kind, String externalId) {
        if (externalId == null) {
            return null;
        }
        var mapping = mappingRepository.findByConnectionIdAndKindAndExternalId(connectionId, kind, externalId);
        return new AccMappedItemDto(externalId,
                mapping.map(AccExternalMappingEntity::getExternalName).orElse(null),
                mapping.map(AccExternalMappingEntity::getAccountId).flatMap(accountService::findAccountName)
                        .orElse(null));
    }

    private AccImportRecordDto getRecord(Long connectionId, Long recordId) {
        return importRecordRepository.findByIdAndConnectionId(recordId, connectionId)
                .map(importRecordMapper::toDto)
                .orElseThrow();
    }
}
