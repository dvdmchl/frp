package org.dreamabout.sw.frp.be.module.common.model.dto;

import org.dreamabout.sw.frp.be.module.common.domain.ModuleState;

public record ModuleDefinitionDto(
    String code,
    String title,
    String description,
    ModuleState state
) {}
