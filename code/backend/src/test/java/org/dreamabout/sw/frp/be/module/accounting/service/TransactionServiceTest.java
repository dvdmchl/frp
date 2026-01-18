package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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

        var expectedDto = new AccTransactionDto(1L, "REF123", "Txn Desc", BigDecimal.ONE, List.of());

        when(accAccountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(accTransactionRepository.save(any(AccTransactionEntity.class))).thenReturn(savedTxn);
        when(transactionMapper.toDto(savedTxn)).thenReturn(expectedDto);

        var result = transactionService.createTransaction(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(accTransactionRepository).save(any(AccTransactionEntity.class));
    }
}
