package org.dreamabout.sw.frp.be;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccJournalRepository;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultitenancyTest extends AbstractDbTest {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccJournalRepository accJournalRepository;

    @Autowired
    private TenantUtil tenantUtil;

    @Autowired
    private SecurityContextService securityContextService;


    @Autowired
    private org.dreamabout.sw.frp.be.module.common.service.GroupService groupService;

    @Test
    void correctSchemaUsingTest() {
        var user = new UserEntity();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user = userRepository.save(user);

        var schema = schemaService.createSchema("frp_test", user.getId());

        user.setSchema(schema);
        user = userRepository.save(user);

        securityContextService.clearContext();
        var auth = new TestingAuthenticationToken(user, null, List.of());
        securityContextService.setAuthentication(auth);
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());
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

    @Test
    void groupAccessTest() {
        // User 1 creates schema
        var user1 = new UserEntity();
        user1.setEmail("owner@test.com");
        user1.setPassword("pass");
        user1 = userRepository.save(user1);

        var schema = schemaService.createSchema("group_schema", user1.getId());

        // User 2 belongs to a group
        var user2 = new UserEntity();
        user2.setEmail("member@test.com");
        user2.setPassword("pass");
        user2 = userRepository.save(user2);

        var group = groupService.createGroup("test_group", "test group");
        groupService.addUserToGroup(user2.getId(), group.getId());

        // Grant access to group
        groupService.grantSchemaAccessToGroup(schema.getName(), group.getId(), org.dreamabout.sw.frp.be.module.common.model.AccessLevel.VIEWER, user1.getId());

        // User 2 should be able to set active schema
        securityContextService.clearContext();
        var auth = new org.springframework.security.authentication.TestingAuthenticationToken(user2, null, List.of());
        securityContextService.setAuthentication(auth);

        // This should not throw exception
        schemaService.setActiveSchema(schema.getName(), user2.getId());

        var updatedUser2 = userRepository.findById(user2.getId()).orElseThrow();
        assertThat(updatedUser2.getSchema().getName()).isEqualTo(schema.getName());
    }
}
