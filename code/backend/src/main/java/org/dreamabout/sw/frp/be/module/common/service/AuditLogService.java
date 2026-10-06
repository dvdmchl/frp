package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.frp.be.module.common.model.AuditLogEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.model.dto.AuditLogDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.AuditLogPageDto;
import org.dreamabout.sw.frp.be.module.common.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    static final int MAX_PAGE_SIZE = 200;
    private static final String CREATED_AT = "createdAt";

    private final AuditLogRepository auditLogRepository;
    private final SecurityContextService securityContextService;

    /**
     * Stores the event in its own transaction; it is called after the audited transaction committed.
     * Without an explicit user in the event, the currently authenticated user (if any) is recorded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(AuditEvent event) {
        var userId = event.userId();
        var userEmail = event.userEmail();
        if (userId == null && userEmail == null) {
            var authentication = securityContextService.getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) {
                userId = user.getId();
                userEmail = user.getEmail();
            }
        }
        auditLogRepository.save(AuditLogEntity.builder()
                .createdAt(Instant.now())
                .userId(userId)
                .userEmail(userEmail)
                .action(event.action())
                .resource(event.resource())
                .details(event.details())
                .build());
    }

    @Transactional(readOnly = true)
    public AuditLogPageDto search(AuditAction action, Long userId, Instant from, Instant to, int page, int size) {
        var pageNumber = Math.max(page, 0);
        var pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        var specs = new ArrayList<Specification<AuditLogEntity>>();
        if (action != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (userId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (from != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(CREATED_AT), from));
        }
        if (to != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get(CREATED_AT), to));
        }
        var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc(CREATED_AT), Sort.Order.desc("id")));
        var result = auditLogRepository.findAll(Specification.allOf(specs), pageRequest);
        List<AuditLogDto> items = result.getContent().stream().map(AuditLogService::toDto).toList();
        return new AuditLogPageDto(items, pageNumber, pageSize, result.getTotalElements(), result.getTotalPages());
    }

    private static AuditLogDto toDto(AuditLogEntity entity) {
        return new AuditLogDto(entity.getId(), entity.getCreatedAt(), entity.getUserId(), entity.getUserEmail(),
                entity.getAction(), entity.getResource(), entity.getDetails());
    }
}
