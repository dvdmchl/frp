package org.dreamabout.sw.frp.be.module.accounting.model.mapper;

import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImportRecordMapper {
    AccImportRecordDto toDto(AccImportRecordEntity entity);
}
