package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;

import java.util.Map;
import java.util.Set;

/**
 * Snapshot of a connection's mappings, fallback accounts and the existing currencies that resolves staged records.
 *
 * @param accounts   account mappings by external id
 * @param categories category mappings by external id
 */
record ConnectionMappings(
        Map<String, AccExternalMappingEntity> accounts,
        Map<String, AccExternalMappingEntity> categories,
        Long fallbackExpenseAccountId,
        Long fallbackRevenueAccountId,
        Set<String> currencyCodes) {

    ConnectionMappings {
        accounts = Map.copyOf(accounts);
        categories = Map.copyOf(categories);
        currencyCodes = Set.copyOf(currencyCodes);
    }

    /**
     * Records of an ignored account or category are skipped; a record in a currency that does not exist is an error.
     * A record is mapped when its account is mapped and, unless it is a transfer between own accounts, its category is
     * mapped or the connection has a fallback for the direction of the amount; otherwise it waits as unmapped.
     */
    RecordResolution resolve(AccImportRecordEntity importRecord) {
        var account = accounts.get(importRecord.getExternalAccountId());
        var category = categoryOf(importRecord);
        if (isIgnored(account) || isIgnored(category)) {
            return RecordResolution.ignored();
        }
        if (!currencyCodes.contains(importRecord.getCurrencyCode())) {
            return RecordResolution.missingCurrency(importRecord.getCurrencyCode());
        }
        if (account == null || account.getAccountId() == null) {
            return RecordResolution.unmapped();
        }
        if (isTransfer(importRecord)) {
            return RecordResolution.mapped(account.getAccountId(), null);
        }
        Long counterAccountId = counterAccountOf(importRecord, category);
        return counterAccountId == null
                ? RecordResolution.unmapped() : RecordResolution.mapped(account.getAccountId(), counterAccountId);
    }

    /**
     * Category mapping of the record; {@code null} for an uncategorized record or a transfer, which needs none.
     */
    private AccExternalMappingEntity categoryOf(AccImportRecordEntity importRecord) {
        if (isTransfer(importRecord) || importRecord.getExternalCategoryId() == null) {
            return null;
        }
        return categories.get(importRecord.getExternalCategoryId());
    }

    private Long counterAccountOf(AccImportRecordEntity importRecord, AccExternalMappingEntity category) {
        if (category != null && category.getAccountId() != null) {
            return category.getAccountId();
        }
        return importRecord.getAmount().signum() > 0 ? fallbackRevenueAccountId : fallbackExpenseAccountId;
    }

    private static boolean isTransfer(AccImportRecordEntity importRecord) {
        return importRecord.getTransferLinkId() != null;
    }

    private static boolean isIgnored(AccExternalMappingEntity mapping) {
        return mapping != null && Boolean.TRUE.equals(mapping.getIgnored());
    }
}
