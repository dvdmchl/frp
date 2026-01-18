package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccJournalDto(
    Long id,
    LocalDate date,
    String description,
    Long accountId,
    BigDecimal credit,
    BigDecimal debit
) {}
