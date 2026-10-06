package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;

/**
 * What the mappings say about a staged record: the status it gets and, when it is mapped, its accounts.
 */
record RecordResolution(ImportRecordStatus status, String errorMessage, Long accountId, Long counterAccountId) {

    static RecordResolution ignored() {
        return new RecordResolution(ImportRecordStatus.SKIPPED, null, null, null);
    }

    static RecordResolution missingCurrency(String currencyCode) {
        return new RecordResolution(ImportRecordStatus.ERROR, "Currency " + currencyCode + " does not exist", null, null);
    }

    static RecordResolution unmapped() {
        return new RecordResolution(ImportRecordStatus.NEW, null, null, null);
    }

    static RecordResolution mapped(Long accountId, Long counterAccountId) {
        return new RecordResolution(ImportRecordStatus.NEW, null, accountId, counterAccountId);
    }

    boolean isMapped() {
        return accountId != null;
    }
}
