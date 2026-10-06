package org.dreamabout.sw.frp.be.module.accounting.model.dto;

/**
 * Fallback accounts of a connection for records whose category is not mapped; {@code null} removes a fallback.
 *
 * @param expenseAccountId EXPENSE account for outgoing records
 * @param revenueAccountId REVENUE account for incoming records
 */
public record AccConnectionFallbackRequestDto(
    Long expenseAccountId,
    Long revenueAccountId
) {}
