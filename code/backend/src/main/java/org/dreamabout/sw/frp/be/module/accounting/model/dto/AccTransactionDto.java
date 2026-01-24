package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccTransactionDto(
    Long id,
    String reference,
    String description,
    BigDecimal fxRate,
    BigDecimal totalAmount,
    List<AccJournalDto> journals
) {}
