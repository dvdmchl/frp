package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccTransactionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.TransactionMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccTransactionRepository accTransactionRepository;
    private final AccAccountRepository accAccountRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public List<AccTransactionDto> getAllTransactions() {
        return accTransactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccTransactionDto getTransaction(Long id) {
        return accTransactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    @Transactional
    public AccTransactionDto createTransaction(AccTransactionCreateRequestDto request) {
        AccTransactionEntity transaction = new AccTransactionEntity();
        transaction.setReference(request.reference());
        transaction.setDescription(request.description());
        transaction.setFxRate(request.fxRate());
        transaction.setJournals(new ArrayList<>());

        for (AccJournalCreateRequestDto journalDto : request.journals()) {
            AccAccountEntity account = accAccountRepository.findById(journalDto.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + journalDto.accountId()));

            AccJournalEntity journal = new AccJournalEntity();
            journal.setDate(journalDto.date());
            journal.setDescription(journalDto.description());
            journal.setAccount(account);
            journal.setCredit(journalDto.credit());
            journal.setDebit(journalDto.debit());
            journal.setTransaction(transaction);

            transaction.getJournals().add(journal);
        }

        return transactionMapper.toDto(accTransactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(Long id) {
        if (!accTransactionRepository.existsById(id)) {
            throw new IllegalArgumentException("Transaction not found");
        }
        accTransactionRepository.deleteById(id);
    }

    @Transactional
    public AccTransactionDto updateTransaction(Long id, AccTransactionCreateRequestDto request) {
        AccTransactionEntity transaction = accTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.setReference(request.reference());
        transaction.setDescription(request.description());
        transaction.setFxRate(request.fxRate());
        
        // Clear existing journals
        transaction.getJournals().clear();

        // Add new journals
        for (AccJournalCreateRequestDto journalDto : request.journals()) {
            AccAccountEntity account = accAccountRepository.findById(journalDto.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + journalDto.accountId()));

            AccJournalEntity journal = new AccJournalEntity();
            journal.setDate(journalDto.date());
            journal.setDescription(journalDto.description());
            journal.setAccount(account);
            journal.setCredit(journalDto.credit());
            journal.setDebit(journalDto.debit());
            journal.setTransaction(transaction);

            transaction.getJournals().add(journal);
        }

        return transactionMapper.toDto(accTransactionRepository.save(transaction));
    }
}
