package org.dreamabout.sw.frp.be.module.accounting;

import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
    "frp.accounting.base-currency=EUR"
})
class AccountingSchemaCreationTest extends AbstractDbTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createSchema_shouldCreateBaseCurrency() {
        UserEntity user = new UserEntity();
        user.setEmail("test_acc@example.com");
        user.setPassword("password");
        user.setFullName("Test User");
        user = userRepository.save(user);

        String schemaName = "acc_test_schema";
        schemaService.createSchema(schemaName, user.getId());

        // Verify currency exists in the new schema
        String sql = "SELECT * FROM " + schemaName + ".acc_currency WHERE code = ?";
        var result = jdbcTemplate.queryForList(sql, "EUR");

        assertThat(result).hasSize(1);
        Map<String, Object> currency = result.get(0);
        assertThat(currency.get("code")).isEqualTo("EUR");
        assertThat(currency.get("is_base")).isEqualTo(true);
        assertThat(currency.get("created_by_user_id")).isEqualTo(user.getId());
    }
}
