package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.CurrencyMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private AccCurrencyRepository accCurrencyRepository;
    @Mock
    private AccAccountRepository accAccountRepository;
    @Mock
    private AccJournalRepository accJournalRepository;
    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void createCurrency_shouldThrowIfCodeExists() {
        var request = new AccCurrencyCreateRequestDto("USD", "US Dollar", false, 2);
        when(accCurrencyRepository.findByCode("USD")).thenReturn(Optional.of(new AccCurrencyEntity()));

        assertThrows(IllegalArgumentException.class, () -> currencyService.createCurrency(request));
    }

    @Test
    void createCurrency_shouldCreateNewCurrency() {
        var request = new AccCurrencyCreateRequestDto("USD", "US Dollar", false, 2);
        var savedEntity = new AccCurrencyEntity();
        savedEntity.setId(1L);
        var expectedDto = new AccCurrencyDto(1L, "USD", "US Dollar", false, 2);

        when(accCurrencyRepository.findByCode("USD")).thenReturn(Optional.empty());
        when(accCurrencyRepository.save(any(AccCurrencyEntity.class))).thenReturn(savedEntity);
        when(currencyMapper.toDto(savedEntity)).thenReturn(expectedDto);

        var result = currencyService.createCurrency(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(accCurrencyRepository).save(any(AccCurrencyEntity.class));
    }

    @Test
    void createCurrency_shouldThrowIfIsBaseAndJournalEntriesExist() {
        var request = new AccCurrencyCreateRequestDto("USD", "US Dollar", true, 2);
        when(accCurrencyRepository.findByCode("USD")).thenReturn(Optional.empty());
        when(accJournalRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> currencyService.createCurrency(request));
    }

    @Test
    void createCurrency_shouldSucceedIfIsBaseAndNoJournalEntries() {
        var request = new AccCurrencyCreateRequestDto("USD", "US Dollar", true, 2);
        var savedEntity = new AccCurrencyEntity();
        savedEntity.setId(1L);
        var expectedDto = new AccCurrencyDto(1L, "USD", "US Dollar", true, 2);

        when(accCurrencyRepository.findByCode("USD")).thenReturn(Optional.empty());
        when(accJournalRepository.count()).thenReturn(0L);
        when(accCurrencyRepository.save(any(AccCurrencyEntity.class))).thenReturn(savedEntity);
        when(currencyMapper.toDto(savedEntity)).thenReturn(expectedDto);

        var result = currencyService.createCurrency(request);

        assertNotNull(result);
        verify(accJournalRepository).count();
    }

    @Test
    void deleteCurrency_shouldThrowIfUsed() {
        var currency = new AccCurrencyEntity();
        currency.setId(1L);
        
        when(accCurrencyRepository.findById(1L)).thenReturn(Optional.of(currency));
        when(accAccountRepository.existsByCurrency(currency)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> currencyService.deleteCurrency(1L));
    }

    @Test
    void setBaseCurrency_shouldThrowIfJournalEntriesExist() {
        var currency = new AccCurrencyEntity();
        currency.setId(1L);
        currency.setIsBase(false);

        when(accCurrencyRepository.findById(1L)).thenReturn(Optional.of(currency));
        when(accJournalRepository.count()).thenReturn(10L);

        assertThrows(IllegalStateException.class, () -> currencyService.setBaseCurrency(1L));
    }

    @Test
    void setBaseCurrency_shouldSucceedIfNoJournalEntries() {
        var currency = new AccCurrencyEntity();
        currency.setId(1L);
        currency.setIsBase(false);

        when(accCurrencyRepository.findById(1L)).thenReturn(Optional.of(currency));
        when(accJournalRepository.count()).thenReturn(0L);

        currencyService.setBaseCurrency(1L);

        verify(accCurrencyRepository).save(currency);
        assertTrue(currency.getIsBase());
    }

    @Test
    void ensureCurrency_shouldCreateIsoCurrencyWithItsNameAndScaleWhenMissing() {
        when(accCurrencyRepository.findByCode("JPY")).thenReturn(Optional.empty());
        var captor = ArgumentCaptor.forClass(AccCurrencyEntity.class);

        currencyService.ensureCurrency("JPY");

        verify(accCurrencyRepository).save(captor.capture());
        assertEquals("JPY", captor.getValue().getCode());
        assertEquals("Japanese Yen", captor.getValue().getName());
        assertEquals(0, captor.getValue().getScale());
        assertFalse(captor.getValue().getIsBase());
    }

    @Test
    void ensureCurrency_shouldUseCodeAsNameAndDefaultScaleWhenCodeIsNotIso() {
        when(accCurrencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());
        var captor = ArgumentCaptor.forClass(AccCurrencyEntity.class);

        currencyService.ensureCurrency("XYZ");

        verify(accCurrencyRepository).save(captor.capture());
        assertEquals("XYZ", captor.getValue().getName());
        assertEquals(2, captor.getValue().getScale());
    }

    @Test
    void ensureCurrency_shouldNotCreateCurrencyWhenItExists() {
        when(accCurrencyRepository.findByCode("EUR")).thenReturn(Optional.of(new AccCurrencyEntity()));

        currencyService.ensureCurrency("EUR");

        verify(accCurrencyRepository, never()).save(any());
    }
}
