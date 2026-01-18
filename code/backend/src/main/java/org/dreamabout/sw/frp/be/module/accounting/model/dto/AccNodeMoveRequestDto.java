package org.dreamabout.sw.frp.be.module.accounting.model.dto;

public record AccNodeMoveRequestDto(
    Long newParentId,
    Integer newOrderIndex
) {}
