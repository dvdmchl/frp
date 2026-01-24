package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import java.math.BigDecimal;
import java.util.Map;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "currencyCode", source = "currency.code")
    @Mapping(target = "balance", expression = "java(getBalance(entity, balances))")
    AccAccountDto toDto(AccAccountEntity entity, @Context Map<Long, BigDecimal> balances);

    default BigDecimal getBalance(AccAccountEntity entity, Map<Long, BigDecimal> balances) {
        if (balances == null || entity.getId() == null) {
            return BigDecimal.ZERO;
        }
        return balances.getOrDefault(entity.getId(), BigDecimal.ZERO);
    }

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    AccNodeDto toDto(AccNodeEntity entity, @Context Map<Long, BigDecimal> balances);
}
