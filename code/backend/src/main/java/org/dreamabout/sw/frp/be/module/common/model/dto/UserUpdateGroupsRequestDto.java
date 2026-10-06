package org.dreamabout.sw.frp.be.module.common.model.dto;

import java.util.Set;

public record UserUpdateGroupsRequestDto(
        Set<Long> groupIds
) {
    public UserUpdateGroupsRequestDto {
        groupIds = groupIds == null ? null : Set.copyOf(groupIds);
    }
}
