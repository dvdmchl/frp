package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeMoveRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.AccountMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private AccJournalRepository accJournalRepository;
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
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenReturn(expectedDto);

        // Act
        var result = accountService.createAccount(request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(accCurrencyRepository).findByCode("CZK");
        verify(accAccountRepository).save(any(AccAccountEntity.class));
        verify(accNodeRepository).save(any(AccNodeEntity.class));
    }

    @Test
    void createAccount_placeholder_shouldCreateAccountWithoutCurrency() {
        // Arrange
        var request = new AccAccountCreateRequestDto(
                null, "Placeholder", "Desc", null, false, AccAcountType.ASSET, true
        );

        var savedAccount = new AccAccountEntity();
        savedAccount.setId(1L);
        var savedNode = new AccNodeEntity();
        savedNode.setId(10L);
        savedNode.setAccount(savedAccount);

        when(accAccountRepository.save(any(AccAccountEntity.class))).thenReturn(savedAccount);
        when(accNodeRepository.save(any(AccNodeEntity.class))).thenReturn(savedNode);
        
        var expectedDto = new AccNodeDto(10L, null, true, null, 0, null);
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenReturn(expectedDto);

        // Act
        accountService.createAccount(request);

        // Assert
        ArgumentCaptor<AccAccountEntity> accountCaptor = ArgumentCaptor.forClass(AccAccountEntity.class);
        verify(accAccountRepository).save(accountCaptor.capture());
        
        AccAccountEntity capturedAccount = accountCaptor.getValue();
        assertThat(capturedAccount.getName()).isEqualTo("Placeholder");
        assertThat(capturedAccount.getCurrency()).isNull();
    }

    @Test
    void getTree_shouldReturnCorrectHierarchy() {
        var rootNode = new AccNodeEntity();
        rootNode.setId(1L);
        rootNode.setOrderIndex(0);

        var childNode = new AccNodeEntity();
        childNode.setId(2L);
        childNode.setParent(rootNode);
        childNode.setOrderIndex(0);

        when(accNodeRepository.findAll()).thenReturn(List.of(rootNode, childNode));
        when(accJournalRepository.findBalances()).thenReturn(List.of());
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenAnswer(inv -> {
             AccNodeEntity n = inv.getArgument(0);
             return new AccNodeDto(n.getId(), n.getParent() != null ? n.getParent().getId() : null, true, null, 0, null);
        });

        var tree = accountService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).id()).isEqualTo(1L);
        assertThat(tree.get(0).children()).hasSize(1);
        assertThat(tree.get(0).children().get(0).id()).isEqualTo(2L);
    }

    @Test
    void moveNode_shouldUpdateOrderIndices() {
        var node = new AccNodeEntity();
        node.setId(1L);
        node.setOrderIndex(1);

        var sibling1 = new AccNodeEntity();
        sibling1.setId(2L);
        sibling1.setOrderIndex(0);

        var sibling2 = new AccNodeEntity();
        sibling2.setId(3L);
        sibling2.setOrderIndex(2);

        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accNodeRepository.findAllByParentIsNullOrderByOrderIndexAsc()).thenReturn(List.of(sibling1, node, sibling2));

        var moveRequest = new AccNodeMoveRequestDto(null, 0);
        accountService.moveNode(1L, moveRequest);

        assertThat(node.getOrderIndex()).isEqualTo(0);
        verify(accNodeRepository).save(node);
    }

    @Test
    void deleteNode_shouldFailIfChildrenExist() {
        var node = new AccNodeEntity();
        node.setId(1L);
        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(new AccNodeEntity()));

        assertThatThrownBy(() -> accountService.deleteNode(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("children");
    }

    @Test
    void deleteNode_ok_test() {
        var node = new AccNodeEntity();
        node.setId(1L);
        var account = new AccAccountEntity();
        node.setAccount(account);

        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        accountService.deleteNode(1L);

        verify(accNodeRepository).delete(node);
        verify(accAccountRepository).delete(account);
    }

    @Test
    void updateAccount_legacyPlaceholderToAccount_test() {
        var node = new AccNodeEntity();
        node.setId(1L);
        node.setIsPlaceholder(true);
        // NO ACCOUNT SET (simulate legacy)

        var request = new AccAccountCreateRequestDto(null, "Name", "Desc", "EUR", true, AccAcountType.EXPENSE, false);
        var currency = new AccCurrencyEntity();
        currency.setCode("EUR");

        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accCurrencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));
        when(accAccountRepository.save(any())).thenAnswer(i -> {
            AccAccountEntity a = i.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(accNodeRepository.save(any())).thenAnswer(i -> i.getArgument(0)); // Return the node
        when(accJournalRepository.findBalanceByAccountId(any())).thenReturn(new Object[]{null, null});
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenReturn(new AccNodeDto(1L, null, false, null, 0, null));

        var result = accountService.updateAccount(1L, request);

        assertThat(node.getIsPlaceholder()).isFalse();
        assertThat(node.getAccount()).isNotNull();
        assertThat(node.getAccount().getName()).isEqualTo("Name");
        verify(accAccountRepository).save(any());
    }

    @Test
    void updateAccount_updatePlaceholderName_test() {
        var node = new AccNodeEntity();
        node.setId(1L);
        node.setIsPlaceholder(true);
        var account = new AccAccountEntity();
        account.setName("Old Name");
        account.setAccountType(AccAcountType.ASSET);
        node.setAccount(account);

        var request = new AccAccountCreateRequestDto(null, "New Name", "Desc", null, false, AccAcountType.ASSET, true);

        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accAccountRepository.save(any())).thenReturn(account);
        when(accNodeRepository.save(any())).thenAnswer(i -> i.getArgument(0)); // Return the node
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenReturn(new AccNodeDto(1L, null, true, null, 0, null));

        accountService.updateAccount(1L, request);

        assertThat(account.getName()).isEqualTo("New Name");
        assertThat(account.getCurrency()).isNull(); // Should remain null
    }

    @Test
    void updateAccount_changeParent_test() {
        var node = new AccNodeEntity();
        node.setId(1L);
        node.setOrderIndex(0);
        
        var oldParent = new AccNodeEntity();
        oldParent.setId(10L);
        node.setParent(oldParent);
        
        var newParent = new AccNodeEntity();
        newParent.setId(20L);
        newParent.setOrderIndex(0);

        var account = new AccAccountEntity();
        account.setId(100L);
        account.setName("Name");
        account.setAccountType(AccAcountType.ASSET);
        node.setAccount(account);
        
        // Sibling in new parent
        var newSibling = new AccNodeEntity();
        newSibling.setId(21L);
        newSibling.setOrderIndex(0);
        newSibling.setParent(newParent);

        var request = new AccAccountCreateRequestDto(20L, "Name", "Desc", "USD", false, AccAcountType.ASSET, false);

        var currency = new AccCurrencyEntity();
        currency.setCode("USD");
        when(accCurrencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));

        when(accNodeRepository.findById(1L)).thenReturn(Optional.of(node));
        when(accAccountRepository.save(any())).thenReturn(account);
        when(accNodeRepository.save(any())).thenAnswer(i -> i.getArgument(0)); 
        when(accountMapper.toDto(any(AccNodeEntity.class), any())).thenReturn(new AccNodeDto(1L, 20L, false, null, 1, null));
        
        // Mock finding new parent
        when(accNodeRepository.findById(20L)).thenReturn(Optional.of(newParent));
        
        // Mock siblings for old parent (shiftOldSiblings)
        when(accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(node)); // Only node itself
        
        // Mock siblings for new parent (getSiblings) to calculate new index
        when(accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(20L)).thenReturn(List.of(newSibling));

        when(accJournalRepository.findBalanceByAccountId(any())).thenReturn(new Object[]{null, null});

        accountService.updateAccount(1L, request);

        assertThat(node.getParent().getId()).isEqualTo(20L);
        assertThat(node.getOrderIndex()).isEqualTo(1); // Should be after newSibling (0) -> 1
    }
}