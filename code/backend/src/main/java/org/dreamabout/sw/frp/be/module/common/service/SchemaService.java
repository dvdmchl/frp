package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final DataSource dataSource;

    @Transactional
    public SchemaEntity createSchema(String schemaName) {
        log.info("Creating schema {}", schemaName);
        createNewSchemaInDatabase(schemaName);

        runFlywayMigrations(schemaName);

        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setName(schemaName);
        var schema = schemaRepository.save(schemaEntity);
        log.info("Schema {} created", schemaName);
        return schema;
    }

    private void createNewSchemaInDatabase(String schemaName) {
        var sql = "CREATE SCHEMA " + schemaName;
        try (var connection = dataSource.getConnection(); var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema: " + schemaName, e);
        }
    }

    private void runFlywayMigrations(String schemaName) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/modules")
                .placeholders(Map.of("schema", schemaName))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
