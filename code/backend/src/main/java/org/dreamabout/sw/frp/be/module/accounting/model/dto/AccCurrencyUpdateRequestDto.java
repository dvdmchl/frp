package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AccCurrencyUpdateRequestDto(
    @NotBlank String name,
    @Min(0) Integer scale
) {}
