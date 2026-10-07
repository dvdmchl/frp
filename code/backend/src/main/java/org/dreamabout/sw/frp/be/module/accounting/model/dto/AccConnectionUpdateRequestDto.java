package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request to change a connection.
 *
 * @param syncIntervalMinutes minutes between scheduled synchronizations; {@code null} keeps the current interval
 */
public record AccConnectionUpdateRequestDto(
    @NotBlank @Size(max = 255) String name,
    Map<String, String> syncSettings,
    @Positive Integer syncIntervalMinutes
) {
    public AccConnectionUpdateRequestDto {
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }
}
