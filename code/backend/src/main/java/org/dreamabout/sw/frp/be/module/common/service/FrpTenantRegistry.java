package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.multitenancy.schema.TenantRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tenant schemas known to FRP: every schema recorded in {@code frp_schema}.
 * Used by the library to migrate all tenant schemas on startup and to find orphan schemas.
 */
@Service
@RequiredArgsConstructor
public class FrpTenantRegistry implements TenantRegistry {

    private final SchemaRepository schemaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getTenantSchemas() {
        return schemaRepository.findAll().stream()
                .map(SchemaEntity::getName)
                .toList();
    }
}
