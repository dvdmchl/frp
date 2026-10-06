package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.connector.AccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.UnknownConnectorTypeException;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.frp.be.module.common.model.AuditLogEntity;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.AuditLogRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(ConnectionServiceTest.FakeConnectorConfig.class)
class ConnectionServiceTest extends AbstractDbTest {

    private static final String SCHEMA = "conn_tenant";
    private static final String TYPE = "FAKE";
    private static final Map<String, String> VALID = Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN);

    @TestConfiguration
    static class FakeConnectorConfig {
        @Bean
        AccountingConnector fakeAccountingConnector() {
            return new FakeAccountingConnector(TYPE, List.of());
        }
    }

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AccConnectionRepository connectionRepository;
    @Autowired
    private ConnectorCredentialCipher cipher;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecurityContextService securityContextService;
    @Autowired
    private TenantUtil tenantUtil;

    private UserEntity owner;

    @BeforeEach
    void setUpTenant() {
        var user = new UserEntity();
        user.setEmail("connections@test.com");
        user.setPassword("password");
        owner = userRepository.save(user);
        owner.setSchema(schemaService.createSchema(SCHEMA, owner.getId()));
        owner = userRepository.save(owner);

        securityContextService.setAuthentication(new TestingAuthenticationToken(owner, null, List.of()));
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        securityContextService.clearContext();
    }

    @Test
    void shouldCreateEnabledConnectionWithEncryptedCredentials() {
        var created = create("Wallet");

        assertThat(created).returns(TYPE, AccConnectionDto::connectorType)
                .returns("Wallet", AccConnectionDto::name)
                .returns(true, AccConnectionDto::enabled)
                .returns(true, AccConnectionDto::credentialsSet)
                .returns(Map.of("intervalHours", "6"), AccConnectionDto::syncSettings);
        var stored = connectionRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getCredentials()).doesNotContain(FakeAccountingConnector.VALID_TOKEN);
        assertThat(cipher.decrypt(new EncryptedCredentials(stored.getCredentialsKeyVersion(), stored.getCredentials())))
                .isEqualTo(new ConnectorCredentials(VALID));
    }

    @Test
    void shouldNotExposeCredentialsInDtoOrRequest() {
        var request = new AccConnectionCreateRequestDto(TYPE, "Wallet", VALID, Map.of());

        var created = connectionService.createConnection(request);

        assertThat(created.toString()).doesNotContain(FakeAccountingConnector.VALID_TOKEN);
        assertThat(request.toString()).doesNotContain(FakeAccountingConnector.VALID_TOKEN);
    }

    @Test
    void shouldRejectUnknownConnectorType() {
        var request = new AccConnectionCreateRequestDto("NOPE", "Wallet", VALID, Map.of());

        assertThatThrownBy(() -> connectionService.createConnection(request))
                .isInstanceOf(UnknownConnectorTypeException.class);
    }

    @Test
    void shouldRejectCredentialsWithMissingField() {
        var request = new AccConnectionCreateRequestDto(TYPE, "Wallet", Map.of(FakeAccountingConnector.TOKEN, " "), Map.of());

        assertThatThrownBy(() -> connectionService.createConnection(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing credential: " + FakeAccountingConnector.TOKEN);
    }

    @Test
    void shouldRejectCredentialsWithUnknownField() {
        var credentials = Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN, "extra", "x");
        var request = new AccConnectionCreateRequestDto(TYPE, "Wallet", credentials, Map.of());

        assertThatThrownBy(() -> connectionService.createConnection(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown credential: extra");
    }

    @Test
    void shouldNotSaveConnectionWhenSourceRejectsCredentials() {
        var request = new AccConnectionCreateRequestDto(TYPE, "Wallet", Map.of(FakeAccountingConnector.TOKEN, "wrong"), Map.of());

        assertThatThrownBy(() -> connectionService.createConnection(request))
                .isInstanceOf(ConnectorAuthException.class);
        assertThat(connectionRepository.count()).isZero();
    }

    @Test
    void shouldRejectDuplicateName() {
        create("Wallet");

        var duplicate = new AccConnectionCreateRequestDto(TYPE, "Wallet", VALID, Map.of());

        assertThatThrownBy(() -> connectionService.createConnection(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldUpdateNameAndSyncSettings() {
        var created = create("Wallet");

        var updated = connectionService.updateConnection(created.id(),
                new AccConnectionUpdateRequestDto("My Wallet", Map.of("intervalHours", "12")));

        assertThat(updated).returns("My Wallet", AccConnectionDto::name)
                .returns(Map.of("intervalHours", "12"), AccConnectionDto::syncSettings);
    }

    @Test
    void shouldRejectRenameToExistingName() {
        create("Wallet");
        var other = create("Other");
        var request = new AccConnectionUpdateRequestDto("Wallet", Map.of());

        var otherId = other.id();

        assertThatThrownBy(() -> connectionService.updateConnection(otherId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRotateCredentials() {
        var created = create("Wallet");
        var before = connectionRepository.findById(created.id()).orElseThrow().getCredentials();

        connectionService.setCredentials(created.id(), VALID);

        assertThat(connectionRepository.findById(created.id()).orElseThrow().getCredentials()).isNotEqualTo(before);
    }

    @Test
    void shouldKeepOldCredentialsWhenNewOnesAreRejected() {
        var created = create("Wallet");
        var before = connectionRepository.findById(created.id()).orElseThrow().getCredentials();
        var wrong = Map.of(FakeAccountingConnector.TOKEN, "wrong");

        var id = created.id();

        assertThatThrownBy(() -> connectionService.setCredentials(id, wrong))
                .isInstanceOf(ConnectorAuthException.class);
        assertThat(connectionRepository.findById(created.id()).orElseThrow().getCredentials()).isEqualTo(before);
    }

    @Test
    void shouldDisableAndEnableConnection() {
        var created = create("Wallet");

        assertThat(connectionService.setEnabled(created.id(), false).enabled()).isFalse();
        assertThat(connectionService.setEnabled(created.id(), true).enabled()).isTrue();
    }

    @Test
    void shouldNotEnableConnectionWithoutCredentials() {
        var created = create("Wallet");
        connectionService.setEnabled(created.id(), false);
        clearCredentials(created.id());
        var id = created.id();

        assertThatThrownBy(() -> connectionService.setEnabled(id, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void shouldDeleteConnection() {
        var created = create("Wallet");

        connectionService.deleteConnection(created.id());

        assertThat(connectionRepository.existsById(created.id())).isFalse();
    }

    @Test
    void shouldFailForMissingConnection() {
        assertThatThrownBy(() -> connectionService.deleteConnection(42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Connection not found");
    }

    @Test
    void shouldAuditCredentialChangesAndDeletionWithoutSecrets() {
        var created = create("Wallet");
        connectionService.setCredentials(created.id(), VALID);
        connectionService.deleteConnection(created.id());

        var entries = auditLogRepository.findAll();

        assertThat(entries).extracting(AuditLogEntity::getAction).containsExactlyInAnyOrder(
                AuditAction.SCHEMA_CREATED, AuditAction.CONNECTION_CREDENTIALS_CHANGED,
                AuditAction.CONNECTION_CREDENTIALS_CHANGED, AuditAction.CONNECTION_DELETED);
        assertThat(entries).filteredOn(e -> e.getAction() != AuditAction.SCHEMA_CREATED)
                .allSatisfy(e -> assertThat(e)
                        .returns("connection:" + SCHEMA + "/" + created.id(), AuditLogEntity::getResource)
                        .returns(owner.getId(), AuditLogEntity::getUserId)
                        .extracting(AuditLogEntity::getDetails).asString()
                        .contains("Wallet").doesNotContain(FakeAccountingConnector.VALID_TOKEN));
    }

    @Test
    void shouldDisableConnectionsAndDropCredentialsInCopiedSchema() {
        var created = create("Wallet");
        markSynced(created.id());

        schemaService.copySchema(SCHEMA, "conn_copy", owner.getId());

        var copy = jdbcTemplate.queryForMap("SELECT * FROM conn_copy.acc_connection WHERE id = ?", created.id());
        assertThat(copy).containsEntry("name", "Wallet")
                .containsEntry("enabled", false)
                .containsEntry("credentials", null)
                .containsEntry("credentials_key_version", null);
        assertThat(connectionRepository.findById(created.id()).orElseThrow())
                .returns(true, AccConnectionEntity::getEnabled)
                .extracting(AccConnectionEntity::getCredentials).isNotNull();
    }

    private AccConnectionDto create(String name) {
        return connectionService.createConnection(
                new AccConnectionCreateRequestDto(TYPE, name, VALID, Map.of("intervalHours", "6")));
    }

    private void clearCredentials(Long id) {
        jdbcTemplate.update("UPDATE " + SCHEMA + ".acc_connection SET credentials = NULL, credentials_key_version = NULL"
                + " WHERE id = ?", id);
    }

    private void markSynced(Long id) {
        var connection = connectionRepository.findById(id).orElseThrow();
        connection.setLastSuccessfulSyncAt(Instant.parse("2026-01-01T00:00:00Z"));
        connection.setSyncState(new HashMap<>(Map.of("cursor", "abc")));
        connectionRepository.save(connection);
    }
}
