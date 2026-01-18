package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.TransactionMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private AccJournalRepository accJournalRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private JournalService journalService;

    @Test
    void updateJournal_shouldUpdateDateAndDescription() {
        var existingJournal = new AccJournalEntity();
        existingJournal.setId(1L);
        existingJournal.setDate(LocalDate.now().minusDays(1));
        existingJournal.setDescription("Old Desc");

        var updateRequest = new AccJournalUpdateRequestDto(LocalDate.now(), "New Desc");
        var updatedDto = new AccJournalDto(1L, LocalDate.now(), "New Desc", 100L, BigDecimal.TEN, BigDecimal.ZERO);

        when(accJournalRepository.findById(1L)).thenReturn(Optional.of(existingJournal));
        when(accJournalRepository.save(existingJournal)).thenReturn(existingJournal);
        when(transactionMapper.toDto(existingJournal)).thenReturn(updatedDto);

        var result = journalService.updateJournal(1L, updateRequest);

        assertNotNull(result);
        assertEquals("New Desc", result.description());
        assertEquals(LocalDate.now(), result.date());
        verify(accJournalRepository).save(existingJournal);
    }
}
