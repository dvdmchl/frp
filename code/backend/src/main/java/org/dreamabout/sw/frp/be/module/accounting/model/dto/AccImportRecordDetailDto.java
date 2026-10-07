package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Staged record with its mappings and its representation in the source, for reviewing it.
 *
 * @param account         external account of the record
 * @param category        external category; {@code null} for an uncategorized record
 * @param transferAccount external account of the other leg of a transfer; {@code null} for a regular record or while
 *                        the other leg is not staged
 * @param baseAmount      amount converted by the source; {@code null} when the source did not convert it
 * @param rawPayload      JSON representation of the record in the source
 */
public record AccImportRecordDetailDto(
    AccImportRecordDto importRecord,
    AccMappedItemDto account,
    AccMappedItemDto category,
    AccMappedItemDto transferAccount,
    BigDecimal baseAmount,
    String baseCurrencyCode,
    Instant sourceUpdatedAt,
    Instant firstSeenAt,
    String rawPayload
) {}
