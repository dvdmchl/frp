package org.dreamabout.sw.frp.be.module.common.model.dto;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String activeSchema
) {
}
