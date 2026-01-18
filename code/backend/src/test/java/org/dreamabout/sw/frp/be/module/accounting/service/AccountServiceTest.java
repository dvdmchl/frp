package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.AccountMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccAccountRepository accAccountRepository;
    @Mock
    private AccNodeRepository accNodeRepository;
    @Mock
    private AccCurrencyRepository accCurrencyRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_shouldCreateNewAccountAndNode() {
        // Arrange
        var request = new AccAccountCreateRequestDto(
                null, "Test Account", "Desc", "CZK", true, AccAcountType.ASSET, false
        );
        var currency = new AccCurrencyEntity();
        currency.setCode("CZK");

        var savedAccount = new AccAccountEntity();
        savedAccount.setId(1L);
        var savedNode = new AccNodeEntity();
        savedNode.setId(10L);
        savedNode.setAccount(savedAccount);

        when(accCurrencyRepository.findByCode("CZK")).thenReturn(Optional.of(currency));
        when(accAccountRepository.save(any(AccAccountEntity.class))).thenReturn(savedAccount);
        when(accNodeRepository.save(any(AccNodeEntity.class))).thenReturn(savedNode);

        var expectedDto = new AccNodeDto(10L, null, false, null, 0, null);
        when(accountMapper.toDto(savedNode)).thenReturn(expectedDto);

        // Act
        var result = accountService.createAccount(request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(accCurrencyRepository).findByCode("CZK");
        verify(accAccountRepository).save(any(AccAccountEntity.class));
        verify(accNodeRepository).save(any(AccNodeEntity.class));
    }
}
