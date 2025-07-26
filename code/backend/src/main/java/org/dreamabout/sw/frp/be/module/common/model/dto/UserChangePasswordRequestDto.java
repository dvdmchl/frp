package org.dreamabout.sw.frp.be.module.common.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Change password request")
public record UserChangePasswordRequestDto(
        @NotBlank
        @Schema(description = "Old password", example = "oldPass123")
        String oldPassword,

        @NotBlank
        @Schema(description = "New password", example = "newPass456")
        String newPassword
) {}
