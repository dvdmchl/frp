package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Connection to an external source. Credentials are write-only: only {@code credentialsSet} tells whether they exist.
 *
 * @param fallbackExpenseAccountId account for outgoing records whose category is not mapped
 * @param fallbackRevenueAccountId account for incoming records whose category is not mapped
 */
public record AccConnectionDto(
    Long id,
    String connectorType,
    String name,
    boolean enabled,
    boolean credentialsSet,
    Map<String, String> syncSettings,
    Instant lastSuccessfulSyncAt,
    Long fallbackExpenseAccountId,
    Long fallbackRevenueAccountId
) {
    public AccConnectionDto {
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }
}
