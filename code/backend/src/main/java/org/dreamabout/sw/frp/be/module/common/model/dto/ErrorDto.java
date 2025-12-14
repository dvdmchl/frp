package org.dreamabout.sw.frp.be.module.common.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response object")
public record ErrorDto(
        @Schema(description = "Type of the error", example = "ClientError")
        String type,
        @Schema(description = "Error message", example = "Validation failed")
        String message,
        @Schema(description = "Stack trace (only in dev mode)", example = "...")
        String stackTrace
) {
}
