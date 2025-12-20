package org.dreamabout.sw.frp.be.module.common.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SchemaCopyRequestDto(@NotBlank String source, @NotBlank String target) {
}
