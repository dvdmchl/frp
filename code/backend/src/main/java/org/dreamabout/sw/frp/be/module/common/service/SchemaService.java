package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.domain.exception.FrpDbException;
import org.dreamabout.sw.frp.be.module.common.model.*;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaAccessRepository;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.multitenancy.schema.SchemaNames;
import org.dreamabout.sw.multitenancy.schema.TenantSchemaManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final SchemaAccessRepository schemaAccessRepository;
    private final UserRepository userRepository;
    private final TenantSchemaManager tenantSchemaManager;
    private final List<SchemaCreationListener> schemaCreationListeners;

    @Transactional
    public SchemaEntity createSchema(String schemaName, Long ownerId) {
        SchemaNames.validate(schemaName);

        if (schemaRepository.findByName(schemaName).isPresent()) {
            throw new IllegalArgumentException("Schema with name " + schemaName + " already exists.");
        }

        log.info("Creating new schema: {}", schemaName);

        tenantSchemaManager.createSchema(schemaName);

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
        SchemaNames.validate(target);

        var sourceSchema = schemaRepository.findByName(source)
                .orElseThrow(() -> new IllegalArgumentException("Source schema not found: " + source));

        checkAccess(sourceSchema, userId, AccessLevel.OWNER);

        if (schemaRepository.findByName(target).isPresent()) {
            throw new IllegalArgumentException("Target schema already exists: " + target);
        }

        log.info("Copying schema from {} to {} for user {}", source, target, userId);

        tenantSchemaManager.copySchema(source, target);

        var schemaEntity = saveSchemaEntity(target, userId);
        grantOwnerAccess(schemaEntity, userId);

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

    @Transactional
    public void deleteSchema(String schemaName, Long userId) {
        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));

        checkAccess(schema, userId, AccessLevel.OWNER);

        if (tenantSchemaManager.schemaExists(schemaName)) {
            tenantSchemaManager.dropSchema(schemaName);
        } else {
            log.warn("Schema {} does not exist in the database, removing only its records", schemaName);
        }

        var accessRecords = schemaAccessRepository.findAllBySchema(schema);
        schemaAccessRepository.deleteAll(accessRecords);

        schemaRepository.delete(schema);
    }

    @Transactional(readOnly = true)
    public List<String> getOrphanSchemas() {
        return tenantSchemaManager.findOrphanSchemas();
    }

    @Transactional
    public void dropOrphanSchemas(List<String> schemaNames) {
        var orphanSchemas = tenantSchemaManager.findOrphanSchemas();
        for (String name : schemaNames) {
            if (!orphanSchemas.contains(name)) {
                throw new IllegalArgumentException("Schema " + name + " is not an orphan schema or doesn't exist.");
            }
            log.info("Dropping orphan schema: {}", name);
            tenantSchemaManager.dropSchema(name);
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
}
