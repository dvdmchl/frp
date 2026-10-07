package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Connection to an external source. Credentials are write-only: only {@code credentialsSet} tells whether they exist.
 *
 * @param fallbackExpenseAccountId account for outgoing records whose category is not mapped
 * @param fallbackRevenueAccountId account for incoming records whose category is not mapped
 * @param syncIntervalMinutes      minutes between scheduled synchronizations
 * @param nextSyncAt               earliest next scheduled synchronization; {@code null} = as soon as possible
 * @param credentialsRejected      the source rejected the credentials; scheduled synchronization waits for new ones
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
    Long fallbackRevenueAccountId,
    int syncIntervalMinutes,
    Instant nextSyncAt,
    boolean credentialsRejected
) {
    public AccConnectionDto {
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }
}
