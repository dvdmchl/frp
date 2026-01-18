package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.domain.exception.FrpDbException;
import org.dreamabout.sw.frp.be.module.common.model.*;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaAccessRepository;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final SchemaAccessRepository schemaAccessRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final SchemaInitializationService schemaInitializationService;
    private final List<TablePriorityProvider> priorityProviders;
    private final List<SchemaCreationListener> schemaCreationListeners;

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    @Transactional
    public SchemaEntity createSchema(String schemaName, Long ownerId) {
        validateSchemaName(schemaName);

        if (schemaRepository.findByName(schemaName).isPresent()) {
            throw new IllegalArgumentException("Schema with name " + schemaName + " already exists.");
        }

        log.info("Creating new schema: {}", schemaName);

        schemaInitializationService.initSchema(schemaName);

        var schema = saveSchemaEntity(schemaName, ownerId);
        grantOwnerAccess(schema, ownerId);

        notifyListeners(schemaName, ownerId);

        return schema;
    }

    @Transactional(readOnly = true)
    public List<SchemaEntity> listMySchemas(Long userId) {
        var user = getUserWithGroups(userId);
        var groupIds = getGroupIds(user);

        return groupIds.isEmpty()
                ? schemaAccessRepository.findDirectAvailableSchemas(userId)
                : schemaAccessRepository.findAvailableSchemas(userId, groupIds);
    }

    @Transactional
    public void setActiveSchema(String schemaName, Long userId) {
        var user = getUserWithGroups(userId);
        var groupIds = getGroupIds(user);

        var accessList = groupIds.isEmpty()
                ? schemaAccessRepository.findDirectAccess(schemaName, userId)
                : schemaAccessRepository.findAccess(schemaName, userId, groupIds);

        if (accessList.isEmpty()) {
            throw new IllegalArgumentException("You do not have access to schema " + schemaName);
        }

        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaName));
        user.setSchema(schema);
        userRepository.save(user);
    }

    @Transactional
    public SchemaEntity copySchema(String source, String target, Long userId) {
        validateSchemaName(target);

        var sourceSchema = schemaRepository.findByName(source)
                .orElseThrow(() -> new IllegalArgumentException("Source schema not found: " + source));

        checkAccess(sourceSchema, userId, AccessLevel.OWNER);

        if (schemaRepository.findByName(target).isPresent()) {
            throw new IllegalArgumentException("Target schema already exists: " + target);
        }

        log.info("Copying schema from {} to {} for user {}", source, target, userId);

        schemaInitializationService.initSchema(target);

        var schemaEntity = saveSchemaEntity(target, userId);
        grantOwnerAccess(schemaEntity, userId);

        copyData(source, target);

        return schemaEntity;
    }

    private UserEntity getUserWithGroups(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Set<Long> getGroupIds(UserEntity user) {
        return user.getGroups().stream()
                .map(GroupEntity::getId)
                .collect(Collectors.toSet());
    }

    private SchemaEntity saveSchemaEntity(String name, Long ownerId) {
        var schemaEntity = new SchemaEntity();
        schemaEntity.setName(name);
        schemaEntity.setOwnerId(ownerId);
        return schemaRepository.save(schemaEntity);
    }

    private void grantOwnerAccess(SchemaEntity schema, Long userId) {
        var user = getUserWithGroups(userId);
        var access = SchemaAccessEntity.builder()
                .schema(schema)
                .user(user)
                .accessLevel(AccessLevel.OWNER)
                .build();
        schemaAccessRepository.save(access);
    }

    private void checkAccess(SchemaEntity schema, Long userId, AccessLevel requiredLevel) {
        var user = getUserWithGroups(userId);
        var groupIds = getGroupIds(user);

        var accessList = groupIds.isEmpty()
                ? schemaAccessRepository.findDirectAccess(schema.getName(), userId)
                : schemaAccessRepository.findAccess(schema.getName(), userId, groupIds);

        boolean hasAccess = accessList.stream().anyMatch(a -> hasLevel(a.getAccessLevel(), requiredLevel));

        if (!hasAccess) {
            throw new IllegalArgumentException("Insufficient access level for schema " + schema.getName());
        }
    }

    private boolean hasLevel(AccessLevel current, AccessLevel required) {
        if (current == AccessLevel.OWNER) return true;
        if (current == AccessLevel.EDITOR && (required == AccessLevel.EDITOR || required == AccessLevel.VIEWER))
            return true;
        return current == AccessLevel.VIEWER && required == AccessLevel.VIEWER;
    }

    private void copyData(String source, String target) {
        // Get all base tables from source (excluding flyway history)
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name != 'flyway_schema_history'",
                String.class, source);

        tables.sort((t1, t2) -> {
            int s1 = getTableScore(t1);
            int s2 = getTableScore(t2);
            if (s1 != s2) return Integer.compare(s1, s2);
            return t1.compareTo(t2);
        });

        for (String table : tables) {
            String sourceTable = "\"" + source + "\".\"" + table + "\"";
            String targetTable = "\"" + target + "\".\"" + table + "\"";

            log.debug("Copying data from {} to {}", sourceTable, targetTable);

            jdbcTemplate.execute("INSERT INTO " + targetTable + " SELECT * FROM " + sourceTable);
        }

        updateSequences(target);
    }

    private int getTableScore(String tableName) {
        return priorityProviders.stream()
                .map(provider -> provider.getTablePriority(tableName))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(10);
    }

    private void updateSequences(String schemaName) {
        // Query to find all sequences in the schema and update them to the max value of the column they are associated with
        String sql = """
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN
                        SELECT
                            s.nspname AS table_schema,
                            t.relname AS table_name,
                            a.attname AS column_name,
                            c.relname AS sequence_name
                        FROM pg_class c
                        JOIN pg_namespace s ON s.oid = c.relnamespace
                        JOIN pg_depend d ON d.objid = c.oid AND d.deptype = 'a'
                        JOIN pg_attribute a ON a.attrelid = d.refobjid AND a.attnum = d.refobjsubid
                        JOIN pg_class t ON t.oid = d.refobjid
                        WHERE c.relkind = 'S'
                          AND s.nspname = '%s'
                    LOOP
                        EXECUTE format('SELECT setval(%%L, (SELECT COALESCE(MAX(%%I), 0) + 1 FROM %%I.%%I), false)',
                                       r.table_schema || '.' || r.sequence_name,
                                       r.column_name,
                                       r.table_schema,
                                       r.table_name);
                    END LOOP;
                END $$;
                """.formatted(schemaName);
        jdbcTemplate.execute(sql);
    }

    @Transactional
    public void deleteSchema(String schemaName, Long userId) {
        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));

        checkAccess(schema, userId, AccessLevel.OWNER);

        jdbcTemplate.execute("DROP SCHEMA " + schemaName + " CASCADE");

        var accessRecords = schemaAccessRepository.findAllBySchema(schema);
        schemaAccessRepository.deleteAll(accessRecords);

        schemaRepository.delete(schema);
    }

    @Transactional(readOnly = true)
    public List<String> getOrphanSchemas() {
        return internalGetOrphanSchemas();
    }

    private List<String> internalGetOrphanSchemas() {
        String sql = """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'public', '%s', '%s')
                  AND schema_name NOT IN (SELECT name FROM %s.frp_schema)
                """.formatted(Constant.PUBLIC_SCHEMA,
                Constant.TEMPLATE_SCHEMA,
                Constant.PUBLIC_SCHEMA);
        return jdbcTemplate.queryForList(sql, String.class);
    }

    @Transactional
    public void dropOrphanSchemas(List<String> schemaNames) {
        List<String> orphanSchemas = internalGetOrphanSchemas();
        for (String name : schemaNames) {
            if (!orphanSchemas.contains(name)) {
                throw new IllegalArgumentException("Schema " + name + " is not an orphan schema or doesn't exist.");
            }
            log.info("Dropping orphan schema: {}", name);
            jdbcTemplate.execute("DROP SCHEMA \"" + name + "\" CASCADE");
        }
    }

    private void notifyListeners(String schemaName, Long ownerId) {
        for (SchemaCreationListener listener : schemaCreationListeners) {
            try {
                listener.onSchemaCreated(schemaName, ownerId);
            } catch (Exception e) {
                log.error("Error in schema creation listener: {}", e.getMessage(), e);
                // We might want to rethrow or just log.
                // If a listener fails (e.g. creating base currency), should schema creation fail?
                // Probably yes, to ensure consistency.
                throw new FrpDbException("Failed to execute schema creation listener", e);
            }
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName + ". Must start with a letter and contain only lowercase letters, numbers, and underscores.");
        }
    }
}