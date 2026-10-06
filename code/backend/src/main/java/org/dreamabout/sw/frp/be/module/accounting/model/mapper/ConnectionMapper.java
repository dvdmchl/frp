package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConnectionMapper {
    @Mapping(target = "credentialsSet", expression = "java(entity.getCredentials() != null)")
    AccConnectionDto toDto(AccConnectionEntity entity);
}
