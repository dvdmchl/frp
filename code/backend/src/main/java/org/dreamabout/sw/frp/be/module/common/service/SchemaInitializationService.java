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

    @org.springframework.beans.factory.annotation.Value("${frp.flyway.clean:false}")
    private boolean clean;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void initSchema(String schemaName) {
        log.info("Running migrations for schema: {}", schemaName);
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .createSchemas(true)
                .cleanDisabled(!clean) // Allow clean if property is true
                .locations("classpath:db/migration/modules/accounting")
                .placeholders(Map.of("schema", schemaName))
                .load();

        try {
            if (clean) {
                log.warn("Cleaning schema {} before migration (frp.flyway.clean=true)", schemaName);
                flyway.clean();
            }
            flyway.migrate();
        } catch (FlywayValidateException _) {
            log.warn("Schema {} validation failed, attempting repair", schemaName);
            flyway.repair();
            flyway.migrate();
        }
    }
}
