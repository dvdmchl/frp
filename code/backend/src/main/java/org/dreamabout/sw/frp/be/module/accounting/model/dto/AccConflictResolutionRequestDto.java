package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotNull;
import org.dreamabout.sw.frp.be.module.accounting.domain.ConflictResolution;

public record AccConflictResolutionRequestDto(
    @NotNull ConflictResolution resolution
) {}
