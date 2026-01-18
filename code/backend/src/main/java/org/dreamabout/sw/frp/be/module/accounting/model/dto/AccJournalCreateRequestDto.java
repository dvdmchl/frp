package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccJournalCreateRequestDto(
    @NotNull LocalDate date,
    String description,
    @NotNull Long accountId,
    @NotNull @Min(0) BigDecimal credit,
    @NotNull @Min(0) BigDecimal debit
) {}
