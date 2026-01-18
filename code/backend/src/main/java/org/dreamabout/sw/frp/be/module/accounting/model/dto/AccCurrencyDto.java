package org.dreamabout.sw.frp.be.module.accounting.model.dto;

public record AccCurrencyDto(
    Long id,
    String code,
    String name,
    Boolean isBase,
    Integer scale
) {}
