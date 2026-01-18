package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {
    AccCurrencyDto toDto(AccCurrencyEntity entity);
}
