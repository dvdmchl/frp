package org.dreamabout.sw.frp.be.module.common.model.dto;

import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Instant createdAt,
        Long userId,
        String userEmail,
        AuditAction action,
        String resource,
        String details
) {
}
