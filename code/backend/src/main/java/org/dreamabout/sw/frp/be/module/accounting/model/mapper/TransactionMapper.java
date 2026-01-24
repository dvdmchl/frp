package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import java.math.BigDecimal;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccTransactionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountId", source = "account.id")
    AccJournalDto toDto(AccJournalEntity entity);

    @Mapping(target = "totalAmount", expression = "java(calculateTotalAmount(entity))")
    AccTransactionDto toDto(AccTransactionEntity entity);

    default BigDecimal calculateTotalAmount(AccTransactionEntity entity) {
        if (entity.getJournals() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getJournals().stream()
                .map(AccJournalEntity::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
