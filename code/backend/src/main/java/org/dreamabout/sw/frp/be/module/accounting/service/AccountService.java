package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String NODE_NOT_FOUND = "Node not found";
    private static final String CURRENCY_NOT_FOUND = "Currency not found: ";

    private final AccAccountRepository accAccountRepository;
    private final AccNodeRepository accNodeRepository;
    private final AccCurrencyRepository accCurrencyRepository;
    private final AccJournalRepository accJournalRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccNodeDto createAccount(AccAccountCreateRequestDto request) {
        AccNodeEntity parent = null;
        if (request.parentId() != null) {
            parent = accNodeRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent node not found"));
        }

        AccNodeEntity node = new AccNodeEntity();
        node.setParent(parent);
        node.setIsPlaceholder(request.isPlaceholder());

        // Add to the end of the list
        List<AccNodeEntity> siblings = request.parentId() == null
                ? accNodeRepository.findAllByParentIsNullOrderByOrderIndexAsc()
                : accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(request.parentId());

        int orderIndex = siblings.isEmpty() ? 0 : siblings.getLast().getOrderIndex() + 1;
        node.setOrderIndex(orderIndex);

        AccAccountEntity account = new AccAccountEntity();
        account.setName(request.name());
        account.setDescription(request.description());
        account.setIsLiquid(request.isLiquid());
        account.setAccountType(request.accountType());

        if (request.currencyCode() != null && !request.currencyCode().isEmpty()) {
            AccCurrencyEntity currency = accCurrencyRepository.findByCode(request.currencyCode())
                    .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND + request.currencyCode()));
            account.setCurrency(currency);
        } else if (Boolean.FALSE.equals(request.isPlaceholder())) {
            throw new IllegalArgumentException("Currency is required for non-placeholder accounts");
        }

        account = accAccountRepository.save(account);
        node.setAccount(account);

        node = accNodeRepository.save(node);
        return accountMapper.toDto(node, Map.of());
    }

    @Transactional(readOnly = true)
    public List<AccNodeDto> getTree() {
        List<AccNodeEntity> allNodes = accNodeRepository.findAll();

        List<Object[]> rawBalances = accJournalRepository.findBalances();
        Map<Long, BigDecimal[]> rawMap = rawBalances.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new BigDecimal[]{(BigDecimal) row[1], (BigDecimal) row[2]}
                ));

        Map<Long, BigDecimal> balances = new java.util.HashMap<>();
        for (AccNodeEntity node : allNodes) {
            if (node.getAccount() != null) {
                Long id = node.getAccount().getId();
                BigDecimal[] sums = rawMap.get(id);
                BigDecimal balance = BigDecimal.ZERO;
                if (sums != null) {
                    balance = calculateBalance(node.getAccount().getAccountType(), sums[0], sums[1]);
                }
                balances.put(id, balance);
            }
        }

        return buildTree(allNodes, balances);
    }

    private List<AccNodeDto> buildTree(List<AccNodeEntity> allNodes, Map<Long, BigDecimal> balances) {
        Map<Long, List<AccNodeEntity>> nodesByParentId = allNodes.stream()
                .filter(n -> n.getParent() != null)
                .collect(Collectors.groupingBy(n -> n.getParent().getId()));

        List<AccNodeEntity> roots = allNodes.stream()
                .filter(n -> n.getParent() == null)
                .sorted((n1, n2) -> Integer.compare(n1.getOrderIndex(), n2.getOrderIndex()))
                .toList();

        return roots.stream()
                .map(root -> mapRecursive(root, nodesByParentId, balances))
                .toList();
    }

    private AccNodeDto mapRecursive(AccNodeEntity node, Map<Long, List<AccNodeEntity>> nodesByParentId, Map<Long, BigDecimal> balances) {
        AccNodeDto dto = accountMapper.toDto(node, balances);
        List<AccNodeEntity> childrenEntities = nodesByParentId.getOrDefault(node.getId(), List.of());

        List<AccNodeDto> childrenDtos = childrenEntities.stream()
                .sorted((n1, n2) -> Integer.compare(n1.getOrderIndex(), n2.getOrderIndex()))
                .map(child -> mapRecursive(child, nodesByParentId, balances))
                .toList();

        return new AccNodeDto(
                dto.id(),
                dto.parentId(),
                dto.isPlaceholder(),
                dto.account(),
                dto.orderIndex(),
                childrenDtos
        );
    }

    private BigDecimal calculateBalance(AccAcountType type, BigDecimal credit, BigDecimal debit) {
        if (credit == null) credit = BigDecimal.ZERO;
        if (debit == null) debit = BigDecimal.ZERO;
        return switch (type) {
            case ASSET, EXPENSE -> debit.subtract(credit);
            default -> credit.subtract(debit);
        };
    }

    @Transactional
    public void moveNode(Long nodeId, AccNodeMoveRequestDto request) {
        AccNodeEntity node = accNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException(NODE_NOT_FOUND));

        AccNodeEntity oldParent = node.getParent();
        AccNodeEntity newParent = resolveNewParent(request.newParentId(), nodeId);

        shiftOldSiblings(node, oldParent);

        List<AccNodeEntity> newSiblings = getSiblings(newParent);
        if (Objects.equals(oldParent, newParent)) {
            newSiblings = newSiblings.stream().filter(n -> !n.getId().equals(nodeId)).toList();
        }

        int targetIndex = calculateTargetIndex(request.newOrderIndex(), newSiblings.size());
        shiftNewSiblings(newSiblings, targetIndex);

        node.setParent(newParent);
        node.setOrderIndex(targetIndex);
        accNodeRepository.save(node);
    }

    private AccNodeEntity resolveNewParent(Long newParentId, Long nodeId) {
        if (newParentId == null) return null;

        AccNodeEntity newParent = accNodeRepository.findById(newParentId)
                .orElseThrow(() -> new IllegalArgumentException("New parent not found"));

        AccNodeEntity current = newParent;
        while (current != null) {
            if (current.getId().equals(nodeId)) {
                throw new IllegalArgumentException("Cannot move node to its own descendant");
            }
            current = current.getParent();
        }
        return newParent;
    }

    private List<AccNodeEntity> getSiblings(AccNodeEntity parent) {
        return parent == null
                ? accNodeRepository.findAllByParentIsNullOrderByOrderIndexAsc()
                : accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(parent.getId());
    }

    private void shiftOldSiblings(AccNodeEntity node, AccNodeEntity oldParent) {
        List<AccNodeEntity> oldSiblings = getSiblings(oldParent);
        for (AccNodeEntity sibling : oldSiblings) {
            if (!sibling.getId().equals(node.getId()) && sibling.getOrderIndex() > node.getOrderIndex()) {
                sibling.setOrderIndex(sibling.getOrderIndex() - 1);
                accNodeRepository.save(sibling);
            }
        }
    }

    private void shiftNewSiblings(List<AccNodeEntity> newSiblings, int targetIndex) {
        for (AccNodeEntity sibling : newSiblings) {
            if (sibling.getOrderIndex() >= targetIndex) {
                sibling.setOrderIndex(sibling.getOrderIndex() + 1);
                accNodeRepository.save(sibling);
            }
        }
    }

    private int calculateTargetIndex(Integer requestedIndex, int maxIndex) {
        if (requestedIndex == null) return maxIndex;
        return Math.clamp(requestedIndex, 0, maxIndex);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        AccNodeEntity node = accNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException(NODE_NOT_FOUND));

        // Check if leaf? Or cascading delete?
        // Requirement says "manage accounts tree". Typically we check for children.
        // If children exist, prevent delete or cascade?
        // For safety, let's prevent delete if children exist.
        List<AccNodeEntity> children = accNodeRepository.findAllByParentIdOrderByOrderIndexAsc(nodeId);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete node with children");
        }

        accNodeRepository.delete(node);
        if (node.getAccount() != null) {
            // Also delete account? Or keep it?
            // Since account is tied to this node, and we deleting the node...
            // Check if account used in transactions? (Foreign key constraints will handle this)
            accAccountRepository.delete(node.getAccount());
        }
    }

    @Transactional
    public AccNodeDto updateAccount(Long nodeId, AccAccountCreateRequestDto request) {
        AccNodeEntity node = accNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException(NODE_NOT_FOUND));

        AccAccountEntity account = node.getAccount();
        if (account == null) {
            // Should not happen for new accounts, but for old placeholders it might.
            account = new AccAccountEntity();
            node.setAccount(account);
        }

        account.setName(request.name());
        account.setDescription(request.description());
        account.setIsLiquid(request.isLiquid());
        account.setAccountType(request.accountType());

        node.setIsPlaceholder(request.isPlaceholder());

        // Handle parent change
        Long currentParentId = node.getParent() == null ? null : node.getParent().getId();
        Long newParentId = request.parentId();

        if (!Objects.equals(currentParentId, newParentId)) {
            AccNodeEntity oldParent = node.getParent();
            AccNodeEntity newParent = resolveNewParent(newParentId, nodeId);

            shiftOldSiblings(node, oldParent);

            // Calculate new order index (append to end)
            List<AccNodeEntity> newSiblings = getSiblings(newParent);
            // If we are moving within same parent (should not happen due to equals check above),
            // we would need to filter out 'node'. But since parent changed, newSiblings doesn't contain 'node'.
            
            int newOrderIndex = newSiblings.isEmpty() ? 0 : newSiblings.getLast().getOrderIndex() + 1;
            
            node.setParent(newParent);
            node.setOrderIndex(newOrderIndex);
        }

        if (request.currencyCode() != null && !request.currencyCode().isEmpty()) {
            if (account.getCurrency() == null || !account.getCurrency().getCode().equals(request.currencyCode())) {
                AccCurrencyEntity currency = accCurrencyRepository.findByCode(request.currencyCode())
                        .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND + request.currencyCode()));
                account.setCurrency(currency);
            }
        } else if (Boolean.FALSE.equals(request.isPlaceholder())) {
            if (account.getCurrency() == null) {
                throw new IllegalArgumentException("Currency is required for non-placeholder accounts");
            }
        }

        account = accAccountRepository.save(account);
        node = accNodeRepository.save(node);

        BigDecimal balance = BigDecimal.ZERO;
        Map<Long, BigDecimal> balances = Map.of();

        if (Boolean.FALSE.equals(node.getIsPlaceholder()) && node.getAccount() != null) {
            Object[] sums = accJournalRepository.findBalanceByAccountId(node.getAccount().getId());
            if (sums != null && sums.length >= 2) {
                BigDecimal credit = (BigDecimal) sums[0];
                BigDecimal debit = (BigDecimal) sums[1];
                balance = calculateBalance(node.getAccount().getAccountType(), credit, debit);
            }
            balances = Map.of(node.getAccount().getId(), balance);
        }
        return accountMapper.toDto(node, balances);
    }
}
