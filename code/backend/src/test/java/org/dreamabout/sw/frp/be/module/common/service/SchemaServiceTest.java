package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
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
        assertThat(orphans).contains("orphan_1");
        assertThat(orphans).doesNotContain("tracked_1");
        assertThat(orphans).doesNotContain(Constant.TEMPLATE_SCHEMA);

        // Drop orphans
        schemaService.dropOrphanSchemas(List.of("orphan_1"));
        
        orphans = schemaService.getOrphanSchemas();
        assertThat(orphans).doesNotContain("orphan_1");

        // Clean up template schema
        jdbcTemplate.execute("DROP SCHEMA " + Constant.TEMPLATE_SCHEMA + " CASCADE");
    }
}