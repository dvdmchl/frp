package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record AccConnectionUpdateRequestDto(
    @NotBlank @Size(max = 255) String name,
    Map<String, String> syncSettings
) {
    public AccConnectionUpdateRequestDto {
        syncSettings = syncSettings == null ? null : Map.copyOf(syncSettings);
    }
}
