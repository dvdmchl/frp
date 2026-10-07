package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccTransactionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.TransactionMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccTransactionRepository accTransactionRepository;
    @Mock
    private AccAccountRepository accAccountRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createTransaction_shouldCreateTransactionWithJournals() {
        var journalDto = new AccJournalCreateRequestDto(
                LocalDate.now(), "Journal Desc", 100L, BigDecimal.TEN, BigDecimal.ZERO
        );
        var request = new AccTransactionCreateRequestDto(
                "REF123", "Txn Desc", BigDecimal.ONE, List.of(journalDto)
        );

        var account = new AccAccountEntity();
        account.setId(100L);

        var savedTxn = new AccTransactionEntity();
        savedTxn.setId(1L);

        var expectedDto = new AccTransactionDto(1L, "REF123", "Txn Desc", BigDecimal.ONE, BigDecimal.TEN, List.of(), null, null);

        when(accAccountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accTransactionRepository.save(any(AccTransactionEntity.class))).thenReturn(savedTxn);
        when(transactionMapper.toDto(savedTxn)).thenReturn(expectedDto);

        var result = transactionService.createTransaction(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(accTransactionRepository).save(any(AccTransactionEntity.class));
    }

    @Test
    void getAllTransactions_shouldMapAllEntities() {
        var entity = new AccTransactionEntity();
        var dto = new AccTransactionDto(1L, "REF", "Desc", BigDecimal.ONE, BigDecimal.TEN, List.of(), null, null);
        when(accTransactionRepository.findAll()).thenReturn(List.of(entity));
        when(transactionMapper.toDto(entity)).thenReturn(dto);

        var result = transactionService.getAllTransactions();

        assertEquals(List.of(dto), result);
    }

    @Test
    void getTransaction_shouldReturnMappedTransactionWhenFound() {
        var entity = new AccTransactionEntity();
        var dto = new AccTransactionDto(1L, "REF", "Desc", BigDecimal.ONE, BigDecimal.TEN, List.of(), null, null);
        when(accTransactionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(transactionMapper.toDto(entity)).thenReturn(dto);

        var result = transactionService.getTransaction(1L);

        assertSame(dto, result);
    }

    @Test
    void getTransaction_shouldThrowWhenNotFound() {
        when(accTransactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransaction(1L));
    }

    @Test
    void createTransaction_shouldThrowWhenAccountNotFound() {
        var request = requestWithJournalFor(100L);
        when(accAccountRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request));
        verify(accTransactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_shouldDeleteWhenExists() {
        when(accTransactionRepository.existsById(1L)).thenReturn(true);

        transactionService.deleteTransaction(1L);

        verify(accTransactionRepository).deleteById(1L);
    }

    @Test
    void deleteTransaction_shouldThrowWhenNotFound() {
        when(accTransactionRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> transactionService.deleteTransaction(1L));
        verify(accTransactionRepository, never()).deleteById(any());
    }

    @Test
    void updateTransaction_shouldReplaceJournalsAndHeader() {
        var account = new AccAccountEntity();
        account.setId(100L);
        var existing = new AccTransactionEntity();
        existing.setId(1L);
        existing.setJournals(new ArrayList<>(List.of(new AccJournalEntity())));
        var dto = new AccTransactionDto(1L, "REF123", "Txn Desc", BigDecimal.ONE, BigDecimal.TEN, List.of(), null, null);
        when(accTransactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accAccountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accTransactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toDto(existing)).thenReturn(dto);

        var result = transactionService.updateTransaction(1L, requestWithJournalFor(100L));

        assertSame(dto, result);
        assertEquals("REF123", existing.getReference());
        assertEquals(1, existing.getJournals().size());
        assertSame(account, existing.getJournals().getFirst().getAccount());
        assertSame(existing, existing.getJournals().getFirst().getTransaction());
    }

    @Test
    void updateTransaction_shouldThrowWhenNotFound() {
        var request = requestWithJournalFor(100L);
        when(accTransactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> transactionService.updateTransaction(1L, request));
    }

    private static AccTransactionCreateRequestDto requestWithJournalFor(Long accountId) {
        var journalDto = new AccJournalCreateRequestDto(
                LocalDate.of(2026, 1, 15), "Journal Desc", accountId, BigDecimal.TEN, BigDecimal.ZERO
        );
        return new AccTransactionCreateRequestDto("REF123", "Txn Desc", BigDecimal.ONE, List.of(journalDto));
    }
}
