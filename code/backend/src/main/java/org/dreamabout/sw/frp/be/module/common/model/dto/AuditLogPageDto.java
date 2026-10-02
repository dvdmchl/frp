package org.dreamabout.sw.frp.be.module.common.model.dto;

import java.util.List;

public record AuditLogPageDto(
        List<AuditLogDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
