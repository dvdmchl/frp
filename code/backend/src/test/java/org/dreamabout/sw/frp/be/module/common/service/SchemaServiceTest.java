package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaServiceTest extends AbstractDbTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchemaRepository schemaRepository;

    @Autowired
    private FrpTenantRegistry tenantRegistry;

    private UserEntity createOwner(String email) {
        return userRepository.save(UserEntity.builder()
                .email(email)
                .password("pass")
                .fullName("Owner")
                .build());
    }

    private boolean schemaExists(String schemaName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)", Boolean.class, schemaName));
    }

    @Test
    void createSchema_ok_test() {
        var user = UserEntity.builder()
                .email("test@owner.com")
                .password("pass")
                .fullName("Owner")
                .build();
        user = userRepository.save(user);

        var schema = schemaService.createSchema("test_schema", user.getId());

        assertThat(schema)
                .isNotNull()
                .returns("test_schema", SchemaEntity::getName)
                .returns(user.getId(), SchemaEntity::getOwnerId);
    }

    @Test
    void createSchema_invalidName_fail_test() {
        assertThatThrownBy(() -> schemaService.createSchema("1_invalid_name", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid schema name");
    }

    @Test
    void orphanSchemas_test() {
        // Create an orphan schema manually
        jdbcTemplate.execute("CREATE SCHEMA orphan_1");
        // Create a template schema manually
        jdbcTemplate.execute("CREATE SCHEMA " + Constant.TEMPLATE_SCHEMA);
        
        // Create a tracked schema
        var user = UserEntity.builder()
                .email("test@owner2.com")
                .password("pass")
                .fullName("Owner 2")
                .build();
        user = userRepository.save(user);
        schemaService.createSchema("tracked_1", user.getId());

        var orphans = schemaService.getOrphanSchemas();
        assertThat(orphans)
                .contains("orphan_1")
                .doesNotContain("tracked_1", Constant.TEMPLATE_SCHEMA);

        // Drop orphans
        schemaService.dropOrphanSchemas(List.of("orphan_1"));
        
        orphans = schemaService.getOrphanSchemas();
        assertThat(orphans).doesNotContain("orphan_1");

        // Clean up template schema
        jdbcTemplate.execute("DROP SCHEMA " + Constant.TEMPLATE_SCHEMA + " CASCADE");
    }

    @Test
    void copySchema_copiesDataAndResetsSequences_test() {
        var owner = createOwner("copy@owner.com");
        schemaService.createSchema("copy_source", owner.getId());

        var copy = schemaService.copySchema("copy_source", "copy_target", owner.getId());

        assertThat(copy)
                .returns("copy_target", SchemaEntity::getName)
                .returns(owner.getId(), SchemaEntity::getOwnerId);
        assertThat(jdbcTemplate.queryForList("SELECT code FROM copy_target.acc_currency", String.class))
                .containsExactlyElementsOf(jdbcTemplate.queryForList("SELECT code FROM copy_source.acc_currency", String.class))
                .isNotEmpty();

        var maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM copy_target.acc_currency", Long.class);
        var nextId = jdbcTemplate.queryForObject("SELECT nextval('copy_target.acc_currency_id_seq')", Long.class);
        assertThat(nextId).isGreaterThan(maxId);
        assertThat(schemaService.listMySchemas(owner.getId()))
                .extracting(SchemaEntity::getName)
                .contains("copy_source", "copy_target");
    }

    @Test
    void copySchema_withoutOwnerAccess_fail_test() {
        var owner = createOwner("copy-owner@owner.com");
        var other = createOwner("copy-other@owner.com");
        schemaService.createSchema("copy_private", owner.getId());

        var otherId = other.getId();
        assertThatThrownBy(() -> schemaService.copySchema("copy_private", "copy_stolen", otherId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient access level");
        assertThat(schemaExists("copy_stolen")).isFalse();
    }

    @Test
    void deleteSchema_dropsSchemaAndRecords_test() {
        var owner = createOwner("delete@owner.com");
        schemaService.createSchema("delete_me", owner.getId());

        schemaService.deleteSchema("delete_me", owner.getId());

        assertThat(schemaExists("delete_me")).isFalse();
        assertThat(schemaRepository.findByName("delete_me")).isEmpty();
        assertThat(schemaService.listMySchemas(owner.getId())).isEmpty();
    }

    @Test
    void deleteSchema_missingInDatabase_removesRecords_test() {
        var owner = createOwner("delete-missing@owner.com");
        schemaService.createSchema("delete_missing", owner.getId());
        jdbcTemplate.execute("DROP SCHEMA delete_missing CASCADE");

        schemaService.deleteSchema("delete_missing", owner.getId());

        assertThat(schemaRepository.findByName("delete_missing")).isEmpty();
    }

    @Test
    void tenantRegistry_listsRecordedSchemas_test() {
        var owner = createOwner("registry@owner.com");
        schemaService.createSchema("registry_a", owner.getId());
        schemaService.createSchema("registry_b", owner.getId());

        assertThat(tenantRegistry.getTenantSchemas()).containsExactlyInAnyOrder("registry_a", "registry_b");
    }
}
