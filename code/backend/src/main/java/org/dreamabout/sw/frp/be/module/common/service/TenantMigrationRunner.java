package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationRunner implements CommandLineRunner {

    private final SchemaRepository schemaRepository;
    private final SchemaInitializationService schemaInitializationService;

    @Override
    public void run(String... args) {
        log.info("Starting migration of tenant schemas...");
        var schemas = schemaRepository.findAll();
        for (var schema : schemas) {
            try {
                schemaInitializationService.initSchema(schema.getName());
            } catch (Exception e) {
                log.error("Failed to migrate schema: {}", schema.getName(), e);
                // We verify that one failure does not stop the others
            }
        }
        log.info("Tenant schema migration finished. Migrated {} schemas.", schemas.size());
    }
}
