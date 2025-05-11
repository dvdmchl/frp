package org.dreamabout.sw.frp.be.module.common.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request")
public record UserLoginRequestDto(

        @Email
        @NotBlank
        @Schema(description = "User's email", example = "john.doe@example.com")
        String email,

        @NotBlank
        @Schema(description = "User's password", example = "securePass123")
        String password
) {
}
