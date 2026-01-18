package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeMoveRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.AccountMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccCurrencyRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (Boolean.FALSE.equals(request.isPlaceholder())) {
            AccCurrencyEntity currency = accCurrencyRepository.findByCode(request.currencyCode())
                    .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND + request.currencyCode()));

            AccAccountEntity account = new AccAccountEntity();
            account.setName(request.name());
            account.setDescription(request.description());
            account.setCurrency(currency);
            account.setIsLiquid(request.isLiquid());
            account.setAccountType(request.accountType());

            account = accAccountRepository.save(account);
            node.setAccount(account);
        }

        node = accNodeRepository.save(node);
        return accountMapper.toDto(node);
    }

    @Transactional(readOnly = true)
    public List<AccNodeDto> getTree() {
        List<AccNodeEntity> allNodes = accNodeRepository.findAll();
        return buildTree(allNodes);
    }

    private List<AccNodeDto> buildTree(List<AccNodeEntity> allNodes) {
        Map<Long, List<AccNodeEntity>> nodesByParentId = allNodes.stream()
                .filter(n -> n.getParent() != null)
                .collect(Collectors.groupingBy(n -> n.getParent().getId()));

        List<AccNodeEntity> roots = allNodes.stream()
                .filter(n -> n.getParent() == null)
                .sorted((n1, n2) -> Integer.compare(n1.getOrderIndex(), n2.getOrderIndex()))
                .toList();

        return roots.stream()
                .map(root -> mapRecursive(root, nodesByParentId))
                .toList();
    }

    private AccNodeDto mapRecursive(AccNodeEntity node, Map<Long, List<AccNodeEntity>> nodesByParentId) {
        AccNodeDto dto = accountMapper.toDto(node);
        List<AccNodeEntity> childrenEntities = nodesByParentId.getOrDefault(node.getId(), List.of());

        List<AccNodeDto> childrenDtos = childrenEntities.stream()
                .sorted((n1, n2) -> Integer.compare(n1.getOrderIndex(), n2.getOrderIndex()))
                .map(child -> mapRecursive(child, nodesByParentId))
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

        // Update basic node props (isPlaceholder) - maybe?
        // Usually we don't change from placeholder to account easily if type strict.
        // Let's assume we can update name/description etc.

        if (Boolean.TRUE.equals(node.getIsPlaceholder())) {
            // It's a placeholder.
            if (Boolean.FALSE.equals(request.isPlaceholder())) {
                // Converting to account
                AccCurrencyEntity currency = accCurrencyRepository.findByCode(request.currencyCode())
                        .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND + request.currencyCode()));

                AccAccountEntity account = new AccAccountEntity();
                account.setName(request.name());
                account.setDescription(request.description());
                account.setCurrency(currency);
                account.setIsLiquid(request.isLiquid());
                account.setAccountType(request.accountType());
                account = accAccountRepository.save(account);
                node.setAccount(account);
                node.setIsPlaceholder(false);
            }
        } else {
            // It's an account.
            AccAccountEntity account = node.getAccount();
            account.setName(request.name());
            account.setDescription(request.description());
            account.setIsLiquid(request.isLiquid());
            account.setAccountType(request.accountType());
            // Currency update? usually restricted if tx exist.
            if (!account.getCurrency().getCode().equals(request.currencyCode())) {
                AccCurrencyEntity currency = accCurrencyRepository.findByCode(request.currencyCode())
                        .orElseThrow(() -> new IllegalArgumentException(CURRENCY_NOT_FOUND + request.currencyCode()));
                account.setCurrency(currency);
            }
            accAccountRepository.save(account);
        }
        return accountMapper.toDto(node);
    }
}
