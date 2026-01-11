package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaInitializationService {

    private final DataSource dataSource;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void initSchema(String schemaName) {
        log.info("Running migrations for schema: {}", schemaName);
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .createSchemas(true)
                .locations("classpath:db/migration/modules")
                .placeholders(Map.of("schema", schemaName))
                .load();

        try {
            flyway.migrate();
        } catch (FlywayValidateException e) {
            log.warn("Schema {} validation failed, attempting repair", schemaName);
            flyway.repair();
            flyway.migrate();
        }
    }
}
