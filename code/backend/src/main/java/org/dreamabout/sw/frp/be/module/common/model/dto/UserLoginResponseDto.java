package org.dreamabout.sw.frp.be.module.common.model.dto;

public record UserLoginResponseDto(
        String token,
        UserDto user
) {
}
