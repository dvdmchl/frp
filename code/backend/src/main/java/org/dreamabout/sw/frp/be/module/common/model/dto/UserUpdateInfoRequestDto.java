package org.dreamabout.sw.frp.be.module.common.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User personal info update request")
public record UserUpdateInfoRequestDto(
        @NotBlank
        @Schema(description = "Full name of the user", example = "John Doe")
        String fullName,

        @Email
        @NotBlank
        @Schema(description = "User's email", example = "john.doe@example.com")
        String email
) {}
