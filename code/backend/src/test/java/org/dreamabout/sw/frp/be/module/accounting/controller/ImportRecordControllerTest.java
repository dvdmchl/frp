package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.domain.ConflictResolution;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConflictResolutionRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDetailDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccMappedItemDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportRecordControllerTest extends AbstractConnectionApiTest {

    private static final String RECORDS = CONNECTIONS + "/1/records";

    @Test
    void shouldListRecordsInRequestedStatuses() throws Exception {
        when(importRecordService.getRecords(1L, Set.of(ImportRecordStatus.ERROR, ImportRecordStatus.CONFLICT)))
                .thenReturn(List.of(recordIn(ImportRecordStatus.ERROR)));

        perform(get(RECORDS).param("status", "ERROR", "CONFLICT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ERROR"))
                .andExpect(jsonPath("$[0].errorMessage").value("Failed"));
    }

    @Test
    void shouldGetRecordDetail() throws Exception {
        when(importRecordService.getRecordDetail(1L, 5L)).thenReturn(new AccImportRecordDetailDto(
                recordIn(ImportRecordStatus.ERROR), new AccMappedItemDto("acc-cash", "Cash", "Wallet cash"), null, null,
                null, null, null, Instant.parse("2026-01-15T10:00:00Z"), "{\"note\":\"Lunch\"}"));

        perform(get(RECORDS + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importRecord.externalId").value("r1"))
                .andExpect(jsonPath("$.account.externalName").value("Cash"))
                .andExpect(jsonPath("$.account.accountName").value("Wallet cash"))
                .andExpect(jsonPath("$.rawPayload").value("{\"note\":\"Lunch\"}"));
    }

    @Test
    void shouldRetryRecord() throws Exception {
        when(importRecordService.retry(1L, 5L)).thenReturn(recordIn(ImportRecordStatus.POSTED));

        perform(post(RECORDS + "/5/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void shouldIgnoreRecord() throws Exception {
        when(importRecordService.ignore(1L, 5L)).thenReturn(recordIn(ImportRecordStatus.SKIPPED));

        perform(post(RECORDS + "/5/ignore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    void shouldResolveConflict() throws Exception {
        when(importRecordService.resolveConflict(1L, 5L, ConflictResolution.USE_SOURCE))
                .thenReturn(recordIn(ImportRecordStatus.POSTED));

        perform(post(RECORDS + "/5/resolve"), new AccConflictResolutionRequestDto(ConflictResolution.USE_SOURCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void shouldRejectResolutionWithoutChoice() throws Exception {
        perform(post(RECORDS + "/5/resolve"), new AccConflictResolutionRequestDto(null))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importRecordService);
    }

    private static AccImportRecordDto recordIn(ImportRecordStatus status) {
        return new AccImportRecordDto(5L, "r1", "acc-cash", "cat-food", LocalDate.of(2026, 1, 15),
                new BigDecimal("-100"), "CZK", "Lunch", "Bistro", ExternalRecordState.BOOKED, null, status,
                status == ImportRecordStatus.ERROR ? "Failed" : null, null, Instant.parse("2026-01-15T10:00:00Z"));
    }
}
