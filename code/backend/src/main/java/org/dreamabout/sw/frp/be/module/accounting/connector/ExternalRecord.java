package org.dreamabout.sw.frp.be.module.accounting.connector;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Transaction record as the source knows it.
 *
 * @param externalId         stable id in the source, key for idempotent import
 * @param externalAccountId  {@link ExternalAccount#externalId()} the record belongs to
 * @param baseAmount         the amount converted by the source to its reference currency, {@code null} when the
 *                           source does not convert; gives the exchange rate of a record in a foreign currency
 * @param externalCategoryId {@link ExternalCategory#externalId()}, {@code null} when uncategorized
 * @param transferLinkId     id shared by both halves of a transfer between own accounts, {@code null} otherwise
 * @param updatedAt          last change in the source, used for incremental sync
 * @param rawPayload         the source representation (JSON)
 */
@Builder
public record ExternalRecord(
        String externalId,
        String externalAccountId,
        LocalDate date,
        ExternalAmount amount,
        ExternalAmount baseAmount,
        String externalCategoryId,
        String note,
        String counterparty,
        ExternalRecordState state,
        String transferLinkId,
        Instant updatedAt,
        String rawPayload) {
}
