package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.domain.exception.FrpDbException;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class SchemaService {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");

    private final SchemaRepository schemaRepository;
    private final UserRepository userRepository;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public SchemaEntity createSchema(String schemaName) {
        validateSchemaName(schemaName);
        log.info("Creating schema {}", schemaName);
        ensureSchemaNotExists(schemaName);
        createNewSchemaInDatabase(schemaName);

        runFlywayMigrations(schemaName);

        return createSchemaEntity(schemaName, "Schema {} created");
    }

    @Transactional(readOnly = true)
    public List<SchemaEntity> listMySchemas(Long userId) {
        return schemaRepository.findAllByCreatedByUserId(userId);
    }

    @Transactional
    public SchemaEntity copySchema(String sourceSchema, String targetSchema, Long userId) {
        validateSchemaName(sourceSchema);
        validateSchemaName(targetSchema);
        var source = schemaRepository.findByName(sourceSchema)
                .orElseThrow(() -> new FrpDbException("Source schema not found: " + sourceSchema));
        if (!source.getCreatedByUserId().equals(userId)) {
            throw new FrpDbException("Not allowed to copy schema: " + sourceSchema);
        }
        ensureSchemaNotExists(targetSchema);

        // create and migrate target schema structure first
        createNewSchemaInDatabase(targetSchema);
        runFlywayMigrations(targetSchema);

        // copy data table-by-table
        copyAllTablesWithData(sourceSchema, targetSchema);

        // persist SchemaEntity for target
        return createSchemaEntity(targetSchema, "Schema {} copied");
    }

    @Transactional
    public void deleteSchema(String schemaName, Long userId) {
        validateSchemaName(schemaName);
        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new FrpDbException("Schema not found: " + schemaName));
        if (!schema.getCreatedByUserId().equals(userId)) {
            throw new FrpDbException("Not allowed to delete schema: " + schemaName);
        }
        // prevent deleting active schema for current user
        var user = getCurrentUser().orElseThrow(() -> new FrpDbException("User not authenticated"));
        if (user.getSchema() != null && user.getSchema().getId().equals(schema.getId())) {
            throw new FrpDbException("Cannot delete active schema. Switch to another schema first.");
        }

        dropSchema(schemaName);
        schemaRepository.delete(schema);
    }

    @Transactional
    public void setActiveSchema(String schemaName, Long userId) {
        validateSchemaName(schemaName);
        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new FrpDbException("Schema not found: " + schemaName));
        if (!schema.getCreatedByUserId().equals(userId)) {
            throw new FrpDbException("Not allowed to use schema: " + schemaName);
        }
        var user = getCurrentUser().orElseThrow(() -> new FrpDbException("User not authenticated"));
        user.setSchema(schema);
        userRepository.save(user);
    }

    private SchemaEntity createSchemaEntity(String schemaName, String logMessage) {
        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setName(schemaName);
        var schema = schemaRepository.save(schemaEntity);
        log.info(logMessage, schemaName);
        return schema;
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new FrpDbException("Invalid schema name");
        }
    }

    private void ensureSchemaNotExists(String schemaName) {
        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
        try {
            var result = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), schemaName);
            if (!result.isEmpty()) {
                throw new FrpDbException("Schema already exists: " + schemaName);
            }
        } catch (Exception e) {
            throw new FrpDbException("Failed to check schema existence: " + schemaName, e);
        }
    }

    private void copyAllTablesWithData(String sourceSchema, String targetSchema) {
        try {
            jdbcTemplate.execute("SET session_replication_role = 'replica'");

            var listSql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type='BASE TABLE' AND table_name != 'flyway_schema_history'";
            List<String> tables = jdbcTemplate.query(listSql, (rs, rowNum) -> rs.getString(1), sourceSchema);

            for (String table : tables) {
                // Truncate target table to ensure clean state (Flyway might have seeded data)
                jdbcTemplate.execute("TRUNCATE TABLE " + targetSchema + "." + table + " CASCADE");

                // Copy data
                var copySql = "INSERT INTO " + targetSchema + "." + table + " SELECT * FROM " + sourceSchema + "." + table;
                jdbcTemplate.update(copySql);
            }
        } catch (Exception e) {
            throw new FrpDbException("Failed to copy schema data from " + sourceSchema + " to " + targetSchema, e);
        } finally {
            // Re-enable FK checks
            try {
                jdbcTemplate.execute("SET session_replication_role = 'origin'");
            } catch (Exception e) {
                log.error("Failed to reset session_replication_role", e);
            }
        }
    }

    private void dropSchema(String schemaName) {
        var sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new FrpDbException("Failed to drop schema: " + schemaName, e);
        }
    }

    private Optional<UserEntity> getCurrentUser() {
        var aut = SecurityContextHolder.getContext().getAuthentication();
        if (aut == null || aut.getPrincipal() == null || !(aut.getPrincipal() instanceof UserEntity)) {
            return Optional.empty();
        }
        var principal = (UserEntity) aut.getPrincipal();
        return userRepository.findById(principal.getId());
    }

    private void createNewSchemaInDatabase(String schemaName) {
        var sql = "CREATE SCHEMA " + schemaName;
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new FrpDbException("Failed to create schema: " + schemaName, e);
        }
    }

    private void runFlywayMigrations(String schemaName) {
        // Use the current transactional connection
        java.sql.Connection connection = DataSourceUtils.getConnection(dataSource);
        
        // Wrap it in a DataSource that Flyway can use, suppressing close to keep it open for transaction
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection, true);

        Flyway.configure()
                .dataSource(singleConnectionDataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/modules")
                .placeholders(Map.of("schema", schemaName))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
