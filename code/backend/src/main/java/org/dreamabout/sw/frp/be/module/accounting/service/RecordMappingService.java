package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccExternalMappingRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the staged records of a connection through its mappings before they are posted.
 */
@Service
@RequiredArgsConstructor
public class RecordMappingService {

    private static final Set<ImportRecordStatus> PENDING = Set.of(ImportRecordStatus.NEW, ImportRecordStatus.ERROR);

    private final ConnectionService connectionService;
    private final AccExternalMappingRepository mappingRepository;
    private final AccImportRecordRepository importRecordRepository;
    private final AccCurrencyRepository currencyRepository;

    /**
     * Resolves the connection's {@link ImportRecordStatus#NEW} and {@link ImportRecordStatus#ERROR} records: records
     * of an ignored account or category become {@link ImportRecordStatus#SKIPPED}, records in a currency missing in
     * the accounting become {@link ImportRecordStatus#ERROR} until it is added, the others are (again)
     * {@link ImportRecordStatus#NEW}. Unmapped records keep waiting; the mapped ones are returned for posting.
     */
    @Transactional
    public List<MappedRecord> applyMappings(Long connectionId) {
        var mappings = mappingsOf(connectionId);
        List<MappedRecord> mapped = new ArrayList<>();
        for (var importRecord : importRecordRepository.findByConnectionIdAndStatusIn(connectionId, PENDING)) {
            var resolution = mappings.resolve(importRecord);
            importRecord.setStatus(resolution.status());
            importRecord.setErrorMessage(resolution.errorMessage());
            if (resolution.isMapped()) {
                mapped.add(new MappedRecord(importRecord.getId(), resolution.accountId(),
                        resolution.counterAccountId()));
            }
        }
        return mapped;
    }

    /**
     * Account the external account of the connection is mapped to; empty when it is unmapped or ignored.
     */
    @Transactional(readOnly = true)
    public Optional<Long> mappedAccountOf(Long connectionId, String externalAccountId) {
        return mappingRepository.findByConnectionIdAndKindAndExternalId(connectionId, ExternalMappingKind.ACCOUNT,
                        externalAccountId)
                .filter(mapping -> !Boolean.TRUE.equals(mapping.getIgnored()))
                .map(AccExternalMappingEntity::getAccountId);
    }

    private ConnectionMappings mappingsOf(Long connectionId) {
        var connection = connectionService.findConnection(connectionId);
        Map<String, AccExternalMappingEntity> accounts = new HashMap<>();
        Map<String, AccExternalMappingEntity> categories = new HashMap<>();
        for (var mapping : mappingRepository.findByConnectionIdOrderByKindAscExternalNameAsc(connectionId)) {
            (mapping.getKind() == ExternalMappingKind.ACCOUNT ? accounts : categories)
                    .put(mapping.getExternalId(), mapping);
        }
        var currencyCodes = currencyRepository.findAll().stream()
                .map(AccCurrencyEntity::getCode)
                .collect(Collectors.toUnmodifiableSet());
        return new ConnectionMappings(accounts, categories, connection.getFallbackExpenseAccountId(),
                connection.getFallbackRevenueAccountId(), currencyCodes);
    }
}
