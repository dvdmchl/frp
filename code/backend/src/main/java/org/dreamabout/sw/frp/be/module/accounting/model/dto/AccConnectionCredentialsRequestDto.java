package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request to set or rotate the credentials of a connection; {@link #toString()} hides the credential values.
 */
public record AccConnectionCredentialsRequestDto(
    @NotNull Map<String, String> credentials
) {
    public AccConnectionCredentialsRequestDto {
        credentials = credentials == null ? null : Map.copyOf(credentials);
    }

    @Override
    public String toString() {
        return "AccConnectionCredentialsRequestDto[credentials="
                + (credentials == null ? null : credentials.keySet()) + "]";
    }
}
