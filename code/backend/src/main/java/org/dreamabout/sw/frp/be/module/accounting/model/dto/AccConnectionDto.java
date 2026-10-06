package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Connection to an external source. Credentials are write-only: only {@code credentialsSet} tells whether they exist.
 */
public record AccConnectionDto(
    Long id,
    String connectorType,
    String name,
    boolean enabled,
    boolean credentialsSet,
    Map<String, String> syncSettings,
    Instant lastSuccessfulSyncAt
) {
    public AccConnectionDto {
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }
}
