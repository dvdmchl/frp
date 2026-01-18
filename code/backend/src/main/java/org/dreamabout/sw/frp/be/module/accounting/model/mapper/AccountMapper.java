package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "currencyCode", source = "currency.code")
    AccAccountDto toDto(AccAccountEntity entity);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    AccNodeDto toDto(AccNodeEntity entity);
}
