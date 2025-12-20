package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final org.dreamabout.sw.frp.be.module.common.repository.UserRepository userRepository;
    private final SchemaInitializationService schemaInitializationService;

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    @Transactional
    public SchemaEntity createSchema(String schemaName, Long ownerId) {
        validateSchemaName(schemaName);

        if (schemaRepository.findByName(schemaName).isPresent()) {
            throw new IllegalArgumentException("Schema with name " + schemaName + " already exists.");
        }

        log.info("Creating new schema: {}", schemaName);
        
        schemaInitializationService.initSchema(schemaName);

        var schema = new SchemaEntity();
        schema.setName(schemaName);
        schema.setOwnerId(ownerId);
        
        return schemaRepository.save(schema);
    }

    public java.util.List<SchemaEntity> listMySchemas(Long userId) {
        return schemaRepository.findAllByOwnerId(userId);
    }

    @Transactional
    public void setActiveSchema(String schemaName, Long userId) {
        var user = userRepository.findById(userId).orElseThrow();
        var schema = schemaRepository.findByName(schemaName).orElseThrow(() -> new IllegalArgumentException("Schema not found"));
        user.setSchema(schema);
        userRepository.save(user);
    }

    @Transactional
    public SchemaEntity copySchema(String source, String target, Long userId) {
        // TODO: Implement copy logic
        throw new UnsupportedOperationException("Copy schema not implemented yet");
    }

    @Transactional
    public void deleteSchema(String schemaName, Long userId) {
        var schema = schemaRepository.findByName(schemaName).orElseThrow();
        if (!schema.getCreatedByUserId().equals(userId)) {
             throw new IllegalArgumentException("Not allowed to delete this schema");
        }
        jdbcTemplate.execute("DROP SCHEMA " + schemaName + " CASCADE");
        schemaRepository.delete(schema);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName + ". Must start with a letter and contain only lowercase letters, numbers, and underscores.");
        }
    }
}