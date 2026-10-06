package org.dreamabout.sw.frp.be.module.accounting.service;

/**
 * Staged record whose accounts are all mapped, so it can be posted.
 *
 * @param accountId        account the external account is mapped to
 * @param counterAccountId account of the category (or the fallback by amount sign); {@code null} for a transfer
 *                         between own accounts, whose counter account is the other leg
 */
public record MappedRecord(Long importRecordId, Long accountId, Long counterAccountId) {
}
