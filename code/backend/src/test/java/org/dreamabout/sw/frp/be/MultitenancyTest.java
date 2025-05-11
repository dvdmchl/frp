package org.dreamabout.sw.frp.be;

import org.dreamabout.sw.frp.be.config.db.TenantContext;
import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultitenancyTest extends AbstractDbTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccJournalRepository accJournalRepository;


    @Test
    void correctSchemaUsingTest() {
        var schema = schemaService.createSchema("frp_test");

        var user = new UserEntity();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setSchema(schema);
        user = userRepository.save(user);

        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        var auth = new TestingAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenant(TenantUtil.getCurrentTenantIdentifier(jdbcTemplate));
        var accJournal = new AccJournalEntity();
        accJournal = accJournalRepository.save(accJournal);

        // user and schema tables must be stored in frp_db/frp_public schema
        var users = selectAllFromPublicSchema(UserEntity.class);
        assertThat(users).hasSize(1);
        var userRes = users.get(0);
        assertThat(userRes.getId()).isEqualTo(user.getId());

        var schemas = selectAllFromPublicSchema(SchemaEntity.class);
        assertThat(schemas).hasSize(1);
        var schemaRes = schemas.get(0);
        assertThat(schemaRes.getId()).isEqualTo(schema.getId());

        // an accounting journal must be stored in frp_test schema
        var accJournals = selectAll(AccJournalEntity.class, schema.getName());
        assertThat(accJournals).hasSize(1);
        var accJournalRes = accJournals.get(0);
        assertThat(accJournalRes.getId()).isEqualTo(accJournal.getId());
    }
}
