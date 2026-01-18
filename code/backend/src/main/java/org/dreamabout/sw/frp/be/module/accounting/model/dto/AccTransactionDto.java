package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccTransactionDto(
    Long id,
    String reference,
    String description,
    BigDecimal fxRate,
    List<AccJournalDto> journals
) {}
