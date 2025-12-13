package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.domain.exception.FrpDbException;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
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
        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement("SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?")) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new FrpDbException("Schema already exists: " + schemaName);
                }
            }
        } catch (Exception e) {
            throw new FrpDbException("Failed to check schema existence: " + schemaName, e);
        }
    }

    private void copyAllTablesWithData(String sourceSchema, String targetSchema) {
        try (var connection = dataSource.getConnection()) {
            // Disable FK checks for the session to allow arbitrary insertion order
            try (var stmt = connection.createStatement()) {
                stmt.execute("SET session_replication_role = 'replica'");
            }

            try (var listStmt = connection.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type='BASE TABLE' AND table_name != 'flyway_schema_history'")) {
                listStmt.setString(1, sourceSchema);
                try (var rs = listStmt.executeQuery()) {
                    while (rs.next()) {
                        var table = rs.getString(1);
                        
                        // Truncate target table to ensure clean state (Flyway might have seeded data)
                        var truncateSql = "TRUNCATE TABLE " + targetSchema + "." + table + " CASCADE";
                        try (var truncateStmt = connection.createStatement()) {
                             truncateStmt.execute(truncateSql);
                        }

                        // Copy data
                        var copySql = "INSERT INTO " + targetSchema + "." + table + " SELECT * FROM " + sourceSchema + "." + table;
                        try (var copyStmt = connection.createStatement()) {
                            copyStmt.executeUpdate(copySql);
                        }
                    }
                }
            } finally {
                // Re-enable FK checks
                try (var stmt = connection.createStatement()) {
                    stmt.execute("SET session_replication_role = 'origin'");
                }
            }
        } catch (Exception e) {
            throw new FrpDbException("Failed to copy schema data from " + sourceSchema + " to " + targetSchema, e);
        }
    }

    private void dropSchema(String schemaName) {
        var sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
        try (var connection = dataSource.getConnection(); var stmt = connection.createStatement()) {
            stmt.execute(sql);
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
        try (var connection = dataSource.getConnection(); var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new FrpDbException("Failed to create schema: " + schemaName, e);
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
