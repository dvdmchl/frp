package org.dreamabout.sw.frp.be.module.common.repository;

import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Multitenant
public interface SchemaRepository extends JpaRepository<SchemaEntity, Long> {
    Optional<SchemaEntity> findByName(String name);
    List<SchemaEntity> findAllByOwnerId(Long ownerId);
}
