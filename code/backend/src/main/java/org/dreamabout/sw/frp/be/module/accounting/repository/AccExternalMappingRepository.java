package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.model.AccExternalMappingEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Multitenant
public interface AccExternalMappingRepository extends JpaRepository<AccExternalMappingEntity, Long> {

    List<AccExternalMappingEntity> findByConnectionIdOrderByKindAscExternalNameAsc(Long connectionId);

    List<AccExternalMappingEntity> findByConnectionIdAndKindAndAccountIdIsNullAndIgnoredFalse(
            Long connectionId, ExternalMappingKind kind);
}
