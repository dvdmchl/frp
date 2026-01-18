package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;

public record AccAccountCreateRequestDto(
    Long parentId,
    String name,
    String description,
    String currencyCode,
    Boolean isLiquid,
    AccAcountType accountType,
    Boolean isPlaceholder
) {}
