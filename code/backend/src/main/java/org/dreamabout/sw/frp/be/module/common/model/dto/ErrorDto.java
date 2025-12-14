package org.dreamabout.sw.frp.be.module.common.model.dto;

public record ErrorDto(
        String type,
        String message,
        String stackTrace
) {
}
