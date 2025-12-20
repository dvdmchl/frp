package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

        assertThat(schema).isNotNull();
        assertThat(schema.getName()).isEqualTo("test_schema");
        assertThat(schema.getOwnerId()).isEqualTo(user.getId());
    }

    @Test
    void createSchema_invalidName_fail_test() {
        assertThatThrownBy(() -> schemaService.createSchema("1_invalid_name", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid schema name");
    }
}