package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record AccTransactionCreateRequestDto(
    String reference,
    String description,
    BigDecimal fxRate,
    @NotEmpty List<AccJournalCreateRequestDto> journals
) {}
