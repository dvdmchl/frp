package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotNull;

public record AccConnectionEnabledRequestDto(
    @NotNull Boolean enabled
) {}
