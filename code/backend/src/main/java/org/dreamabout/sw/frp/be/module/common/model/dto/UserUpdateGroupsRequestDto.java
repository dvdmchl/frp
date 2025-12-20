package org.dreamabout.sw.frp.be.module.common.model.dto;

import java.util.Set;

public record UserUpdateGroupsRequestDto(
        Set<Long> groupIds
) {
}
