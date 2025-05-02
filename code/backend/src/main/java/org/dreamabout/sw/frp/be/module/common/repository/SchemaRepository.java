package org.dreamabout.sw.frp.be.module.common.repository;

import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaRepository extends JpaRepository<SchemaEntity, Long> {
}
