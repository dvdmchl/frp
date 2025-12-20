package org.dreamabout.sw.frp.be.module.common.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SchemaCreateRequestDto(@NotBlank String name) {
}
