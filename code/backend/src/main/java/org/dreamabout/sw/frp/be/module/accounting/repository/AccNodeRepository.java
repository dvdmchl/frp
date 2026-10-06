package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccNodeEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Multitenant
public interface AccNodeRepository extends JpaRepository<AccNodeEntity, Long> {
    List<AccNodeEntity> findAllByParentIdOrderByOrderIndexAsc(Long parentId);
    List<AccNodeEntity> findAllByParentIsNullOrderByOrderIndexAsc();
    Optional<AccNodeEntity> findByAccountId(Long accountId);
}
