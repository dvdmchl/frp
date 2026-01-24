package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.CurrencyMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private static final String CURRENCY_NOT_FOUND = "Currency not found";

    private final AccCurrencyRepository accCurrencyRepository;
    private final AccAccountRepository accAccountRepository;
    private final AccJournalRepository accJournalRepository;
    private final CurrencyMapper currencyMapper;

    @Transactional(readOnly = true)
    public List<AccCurrencyDto> getAllCurrencies() {
        return accCurrencyRepository.findAll().stream()
                .map(currencyMapper::toDto)
                .toList();
    }

    @Transactional
    public AccCurrencyDto createCurrency(AccCurrencyCreateRequestDto request) {
        if (accCurrencyRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Currency with code " + request.code() + " already exists");
        }

        if (Boolean.TRUE.equals(request.isBase())) {
            validateNoJournalEntries();
            unsetExistingBaseCurrency();
        }

        AccCurrencyEntity currency = new AccCurrencyEntity();
        currency.setCode(request.code());
        currency.setName(request.name());
        currency.setScale(request.scale());
        currency.setIsBase(Boolean.TRUE.equals(request.isBase()));

        return currencyMapper.toDto(accCurrencyRepository.save(currency));
    }

    @Transactional
    public AccCurrencyDto updateCurrency(Long id, AccCurrencyUpdateRequestDto request) {
        AccCurrencyEntity currency = accCurrencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND));

        currency.setName(request.name());
        currency.setScale(request.scale());

        return currencyMapper.toDto(accCurrencyRepository.save(currency));
    }

    @Transactional
    public void deleteCurrency(Long id) {
        AccCurrencyEntity currency = accCurrencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND));

        if (accAccountRepository.existsByCurrency(currency)) {
            throw new IllegalStateException("Cannot delete currency as it is used by one or more accounts");
        }

        if (Boolean.TRUE.equals(currency.getIsBase())) {
             throw new IllegalStateException("Cannot delete base currency. Please set another currency as base first.");
        }

        accCurrencyRepository.delete(currency);
    }
    
    @Transactional
    public void setBaseCurrency(Long id) {
        AccCurrencyEntity newBase = accCurrencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND));
        
        if (Boolean.TRUE.equals(newBase.getIsBase())) {
            return;
        }

        validateNoJournalEntries();
        
        unsetExistingBaseCurrency();
        newBase.setIsBase(true);
        accCurrencyRepository.save(newBase);
    }

    private void validateNoJournalEntries() {
        if (accJournalRepository.count() > 0) {
            throw new IllegalStateException("Cannot change base currency when journal entries exist.");
        }
    }

    private void unsetExistingBaseCurrency() {
        accCurrencyRepository.findAll().stream()
                .filter(AccCurrencyEntity::getIsBase)
                .forEach(c -> {
                    c.setIsBase(false);
                    accCurrencyRepository.save(c);
                });
    }
}
