package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AccJournalUpdateRequestDto(
    @NotNull LocalDate date,
    String description
) {}
