package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccSyncRunDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SyncRunMapper {
    AccSyncRunDto toDto(AccSyncRunEntity entity);
}
