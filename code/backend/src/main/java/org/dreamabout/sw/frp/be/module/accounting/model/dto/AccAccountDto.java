package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;

public record AccAccountDto(
    Long id,
    String name,
    String description,
    String currencyCode,
    Boolean isLiquid,
    AccAcountType accountType,
    java.math.BigDecimal balance
) {}
