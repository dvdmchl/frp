package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request to create a connection; {@link #toString()} hides the credential values.
 */
public record AccConnectionCreateRequestDto(
    @NotBlank @Size(max = 50) String connectorType,
    @NotBlank @Size(max = 255) String name,
    @NotNull Map<String, String> credentials,
    Map<String, String> syncSettings
) {
    public AccConnectionCreateRequestDto {
        credentials = credentials == null ? null : Map.copyOf(credentials);
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }

    @Override
    public String toString() {
        return "AccConnectionCreateRequestDto[connectorType=" + connectorType + ", name=" + name
                + ", credentials=" + (credentials == null ? null : credentials.keySet())
                + ", syncSettings=" + syncSettings + "]";
    }
}
