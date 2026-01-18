package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.util.List;

public record AccNodeDto(
    Long id,
    Long parentId,
    Boolean isPlaceholder,
    AccAccountDto account,
    Integer orderIndex,
    List<AccNodeDto> children
) {}
