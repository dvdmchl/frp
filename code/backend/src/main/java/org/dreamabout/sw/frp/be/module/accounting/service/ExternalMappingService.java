package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRegistry;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.ExternalMappingMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccExternalMappingRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps the external accounts and categories of a connection to accounting accounts: an external account to an ASSET
 * or LIABILITY account, an external category to an EXPENSE or REVENUE account.
 */
@Service
@RequiredArgsConstructor
public class ExternalMappingService {

    private final ConnectionService connectionService;
    private final ConnectorRegistry connectorRegistry;
    private final AccountService accountService;
    private final CurrencyService currencyService;
    private final RecordMappingService recordMappingService;
    private final AccExternalMappingRepository mappingRepository;
    private final AccImportRecordRepository importRecordRepository;
    private final ExternalMappingMapper mappingMapper;

    @Transactional(readOnly = true)
    public List<AccExternalMappingDto> getMappings(Long connectionId) {
        return mappingDtos(connectionId);
    }

    /**
     * Fetches the accounts and categories from the source: new ones are added unmapped, known ones get the current
     * name and currency and keep their mapping. Ones the source no longer returns are kept for their staged records.
     */
    @Transactional
    public List<AccExternalMappingDto> refreshMappings(Long connectionId) {
        var connection = connectionService.findConnection(connectionId);
        var connector = connectorRegistry.get(connection.getConnectorType());
        var credentials = connectionService.credentialsOf(connection);

        var accounts = mappingsOfKind(connectionId, ExternalMappingKind.ACCOUNT);
        for (ExternalAccount account : connector.fetchAccounts(credentials)) {
            upsert(accounts, connectionId, ExternalMappingKind.ACCOUNT, account.externalId(), account.name())
                    .setCurrencyCode(account.currencyCode());
        }
        var categories = mappingsOfKind(connectionId, ExternalMappingKind.CATEGORY);
        for (ExternalCategory category : connector.fetchCategories(credentials)) {
            upsert(categories, connectionId, ExternalMappingKind.CATEGORY, category.externalId(), category.name());
        }
        mappingRepository.saveAll(accounts.values());
        mappingRepository.saveAll(categories.values());
        return mappingDtos(connectionId);
    }

    /**
     * Maps (or unmaps) the external account or category and sets its ignore flag. Changing the flag resolves the
     * staged records of the connection again; records skipped because of the flag come back when it is cleared.
     */
    @Transactional
    public AccExternalMappingDto updateMapping(Long connectionId, Long mappingId,
                                               AccExternalMappingUpdateRequestDto request) {
        var mapping = mappingRepository.findById(mappingId)
                .filter(found -> found.getConnectionId().equals(connectionId))
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found"));
        if (request.accountId() != null) {
            accountService.validatePostableAccount(request.accountId(), mapping.getKind().targetTypes());
        }
        boolean wasIgnored = Boolean.TRUE.equals(mapping.getIgnored());
        mapping.setAccountId(request.accountId());
        mapping.setIgnored(request.ignored());
        var saved = mappingRepository.saveAndFlush(mapping);

        if (wasIgnored != request.ignored()) {
            if (wasIgnored) {
                reopenSkipped(saved);
            }
            recordMappingService.applyMappings(saved.getConnectionId());
        }
        return mappingMapper.toDto(saved);
    }

    /**
     * Creates an account under the parent node for every unmapped, not ignored external account or category of the
     * kind and maps it. External accounts become liquid ASSET accounts in their source currency; categories become
     * REVENUE accounts when their staged records add up to income, otherwise EXPENSE accounts, in the base currency.
     * A name already used in the accounting gets a numeric suffix.
     */
    @Transactional
    public List<AccExternalMappingDto> createMissingAccounts(Long connectionId, ExternalMappingKind kind,
                                                             Long parentNodeId) {
        var connection = connectionService.findConnection(connectionId);
        var missing = mappingRepository.findByConnectionIdAndKindAndAccountIdIsNullAndIgnoredFalse(connectionId, kind);
        boolean account = kind == ExternalMappingKind.ACCOUNT;
        var netAmounts = account ? Map.<String, BigDecimal>of() : netAmountByCategory(connectionId);
        for (var mapping : missing) {
            var type = account ? AccAcountType.ASSET : categoryType(netAmounts.get(mapping.getExternalId()));
            var currencyCode = mapping.getCurrencyCode() != null
                    ? mapping.getCurrencyCode() : currencyService.getBaseCurrencyCode();
            var request = new AccAccountCreateRequestDto(parentNodeId,
                    accountService.uniqueAccountName(mapping.getExternalName()),
                    "Imported from " + connection.getName(), currencyCode, account, type, false);
            mapping.setAccountId(accountService.createAccount(request).account().id());
        }
        return mappingRepository.saveAll(missing).stream().map(mappingMapper::toDto).toList();
    }

    private List<AccExternalMappingDto> mappingDtos(Long connectionId) {
        return mappingRepository.findByConnectionIdOrderByKindAscExternalNameAsc(connectionId).stream()
                .map(mappingMapper::toDto)
                .toList();
    }

    private Map<String, AccExternalMappingEntity> mappingsOfKind(Long connectionId, ExternalMappingKind kind) {
        return mappingRepository.findByConnectionIdOrderByKindAscExternalNameAsc(connectionId).stream()
                .filter(mapping -> mapping.getKind() == kind)
                .collect(Collectors.toMap(AccExternalMappingEntity::getExternalId, Function.identity(),
                        (first, second) -> first, HashMap::new));
    }

    private static AccExternalMappingEntity upsert(Map<String, AccExternalMappingEntity> mappings, Long connectionId,
                                                   ExternalMappingKind kind, String externalId, String name) {
        var mapping = mappings.computeIfAbsent(externalId, id -> {
            var created = new AccExternalMappingEntity();
            created.setConnectionId(connectionId);
            created.setKind(kind);
            created.setExternalId(id);
            return created;
        });
        mapping.setExternalName(name);
        return mapping;
    }

    private void reopenSkipped(AccExternalMappingEntity mapping) {
        if (mapping.getKind() == ExternalMappingKind.ACCOUNT) {
            importRecordRepository.reopenSkippedOfAccount(mapping.getConnectionId(), mapping.getExternalId());
        } else {
            importRecordRepository.reopenSkippedOfCategory(mapping.getConnectionId(), mapping.getExternalId());
        }
    }

    private Map<String, BigDecimal> netAmountByCategory(Long connectionId) {
        return importRecordRepository.sumAmountByCategory(connectionId).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (BigDecimal) row[1]));
    }

    private static AccAcountType categoryType(BigDecimal netAmount) {
        return netAmount != null && netAmount.signum() > 0 ? AccAcountType.REVENUE : AccAcountType.EXPENSE;
    }
}
