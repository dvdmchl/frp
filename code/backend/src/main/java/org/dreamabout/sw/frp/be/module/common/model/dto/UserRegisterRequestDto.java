package org.dreamabout.sw.frp.be.module.common.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User registration request")
public record UserRegisterRequestDto(

        @Email
        @NotBlank
        @Schema(description = "User's email", example = "john.doe@example.com")
        String email,

        @NotBlank
        @Schema(description = "Password", example = "securePass123")
        String password,

        @NotBlank
        @Schema(description = "Full name of the user", example = "John Doe")
        String fullName
) {
}
