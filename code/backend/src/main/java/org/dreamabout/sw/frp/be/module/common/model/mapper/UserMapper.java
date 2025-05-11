package org.dreamabout.sw.frp.be.module.common.model.mapper;

import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDto toDto(UserEntity userEntity);
}
