package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExternalMappingMapper {
    AccExternalMappingDto toDto(AccExternalMappingEntity entity);
}
