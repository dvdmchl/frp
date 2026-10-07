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

import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private static final String CURRENCY_NOT_FOUND = "Currency not found";
    private static final int DEFAULT_SCALE = 2;

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

        return currencyMapper.toDto(saveCurrency(request));
    }

    /**
     * Adds the currency when it does not exist yet. Name and scale come from ISO 4217; a code it does not know gets
     * the code as its name and two decimal places.
     */
    @Transactional
    public void ensureCurrency(String code) {
        if (accCurrencyRepository.findByCode(code).isEmpty()) {
            saveCurrency(isoCurrency(code));
        }
    }

    private AccCurrencyEntity saveCurrency(AccCurrencyCreateRequestDto request) {
        AccCurrencyEntity currency = new AccCurrencyEntity();
        currency.setCode(request.code());
        currency.setName(request.name());
        currency.setScale(request.scale());
        currency.setIsBase(Boolean.TRUE.equals(request.isBase()));
        return accCurrencyRepository.save(currency);
    }

    private static AccCurrencyCreateRequestDto isoCurrency(String code) {
        try {
            var currency = Currency.getInstance(code);
            int scale = currency.getDefaultFractionDigits();
            return new AccCurrencyCreateRequestDto(code, currency.getDisplayName(Locale.ENGLISH), false,
                    scale < 0 ? DEFAULT_SCALE : scale);
        } catch (IllegalArgumentException _) {
            return new AccCurrencyCreateRequestDto(code, code, false, DEFAULT_SCALE);
        }
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

    @Transactional(readOnly = true)
    public String getBaseCurrencyCode() {
        return accCurrencyRepository.findByIsBaseTrue()
                .map(AccCurrencyEntity::getCode)
                .orElseThrow(() -> new IllegalStateException("Base currency is not set"));
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
