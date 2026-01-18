package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccCurrencyCreateRequestDto(
    @NotBlank @Size(min = 3, max = 3) String code,
    @NotBlank String name,
    Boolean isBase,
    @Min(0) Integer scale
) {}
