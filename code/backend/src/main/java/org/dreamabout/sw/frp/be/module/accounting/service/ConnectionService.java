package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.connector.AccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRegistry;
import org.dreamabout.sw.frp.be.module.accounting.connector.CredentialField;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionFallbackRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectorDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCredentialFieldDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.ConnectionMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.frp.be.module.common.service.AuditEvent;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the tenant's connections to external sources. Credentials are verified against the connector before they
 * are stored, kept encrypted and never returned; their changes and connection deletions go to the audit log.
 */
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private static final String CONNECTION_NOT_FOUND = "Connection not found";

    private final AccConnectionRepository connectionRepository;
    private final ConnectorRegistry connectorRegistry;
    private final ConnectorCredentialCipher credentialCipher;
    private final ConnectionMapper connectionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountService accountService;

    /**
     * Connector types available for new connections with the credential fields they need.
     */
    public List<AccConnectorDto> getConnectors() {
        return connectorRegistry.types().stream()
                .map(connectorRegistry::get)
                .map(connector -> new AccConnectorDto(connector.type(), connector.credentialFields().stream()
                        .map(field -> new AccCredentialFieldDto(field.name(), field.secret()))
                        .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccConnectionDto> getConnections() {
        return connectionRepository.findAllByOrderByNameAsc().stream().map(connectionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AccConnectionDto getConnection(Long id) {
        return connectionMapper.toDto(findConnection(id));
    }

    /**
     * Verifies the stored credentials against the source; throws the connector exception when
     * the source does not accept them.
     */
    @Transactional(readOnly = true)
    public void testConnection(Long id) {
        var connection = findConnection(id);
        if (connection.getCredentials() == null) {
            throw new IllegalStateException("Connection has no credentials");
        }
        connectorRegistry.get(connection.getConnectorType()).testConnection(credentialsOf(connection));
    }

    @Transactional
    public AccConnectionDto createConnection(AccConnectionCreateRequestDto request) {
        AccountingConnector connector = connectorRegistry.get(request.connectorType());
        if (connectionRepository.existsByName(request.name())) {
            throw duplicateName(request.name());
        }
        ConnectorCredentials credentials = verifiedCredentials(connector, request.credentials());

        var connection = new AccConnectionEntity();
        connection.setConnectorType(connector.type());
        connection.setName(request.name());
        connection.setSyncSettings(syncSettingsOf(request.syncSettings()));
        storeCredentials(connection, credentials);
        var saved = connectionRepository.save(connection);

        audit(AuditAction.CONNECTION_CREDENTIALS_CHANGED, saved);
        return connectionMapper.toDto(saved);
    }

    @Transactional
    public AccConnectionDto updateConnection(Long id, AccConnectionUpdateRequestDto request) {
        var connection = findConnection(id);
        if (connectionRepository.existsByNameAndIdNot(request.name(), id)) {
            throw duplicateName(request.name());
        }
        connection.setName(request.name());
        connection.setSyncSettings(syncSettingsOf(request.syncSettings()));
        if (request.syncIntervalMinutes() != null) {
            connection.setSyncIntervalMinutes(request.syncIntervalMinutes());
        }
        return connectionMapper.toDto(connectionRepository.save(connection));
    }

    /**
     * Sets or rotates the credentials; the stored ones stay unchanged when the source rejects the new ones. Accepted
     * credentials resume a schedule paused because the source rejected the previous ones.
     */
    @Transactional
    public AccConnectionDto setCredentials(Long id, Map<String, String> credentialValues) {
        var connection = findConnection(id);
        var connector = connectorRegistry.get(connection.getConnectorType());
        storeCredentials(connection, verifiedCredentials(connector, credentialValues));
        if (Boolean.TRUE.equals(connection.getCredentialsRejected())) {
            connection.setCredentialsRejected(false);
            connection.setNextSyncAt(null);
        }
        var saved = connectionRepository.save(connection);

        audit(AuditAction.CONNECTION_CREDENTIALS_CHANGED, saved);
        return connectionMapper.toDto(saved);
    }

    @Transactional
    public AccConnectionDto setEnabled(Long id, boolean enabled) {
        var connection = findConnection(id);
        if (enabled && connection.getCredentials() == null) {
            throw new IllegalStateException("Connection cannot be enabled without credentials");
        }
        connection.setEnabled(enabled);
        return connectionMapper.toDto(connectionRepository.save(connection));
    }

    /**
     * Sets the accounts for records whose category is not mapped: an EXPENSE account for outgoing records and a REVENUE
     * account for incoming ones; {@code null} removes a fallback.
     */
    @Transactional
    public AccConnectionDto setFallbackAccounts(Long id, AccConnectionFallbackRequestDto request) {
        var connection = findConnection(id);
        validateFallback(request.expenseAccountId(), AccAcountType.EXPENSE);
        validateFallback(request.revenueAccountId(), AccAcountType.REVENUE);
        connection.setFallbackExpenseAccountId(request.expenseAccountId());
        connection.setFallbackRevenueAccountId(request.revenueAccountId());
        return connectionMapper.toDto(connectionRepository.save(connection));
    }

    @Transactional
    public void deleteConnection(Long id) {
        var connection = findConnection(id);
        connectionRepository.delete(connection);
        audit(AuditAction.CONNECTION_DELETED, connection);
    }

    public AccConnectionEntity findConnection(Long id) {
        return connectionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(CONNECTION_NOT_FOUND));
    }

    /**
     * The connection, provided it may be synchronized.
     *
     * @throws IllegalStateException when the connection is disabled
     */
    public AccConnectionEntity findEnabledConnection(Long id) {
        var connection = findConnection(id);
        if (!Boolean.TRUE.equals(connection.getEnabled())) {
            throw new IllegalStateException("Connection " + id + " is disabled");
        }
        return connection;
    }

    public ConnectorCredentials credentialsOf(AccConnectionEntity connection) {
        return credentialCipher.decrypt(
                new EncryptedCredentials(connection.getCredentialsKeyVersion(), connection.getCredentials()));
    }

    private static ConnectorCredentials verifiedCredentials(AccountingConnector connector, Map<String, String> values) {
        var fields = connector.credentialFields();
        for (String name : values.keySet()) {
            if (fields.stream().noneMatch(field -> field.name().equals(name))) {
                throw new IllegalArgumentException("Unknown credential: " + name);
            }
        }
        for (CredentialField field : fields) {
            String value = values.get(field.name());
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing credential: " + field.name());
            }
        }
        var credentials = new ConnectorCredentials(values);
        connector.testConnection(credentials);
        return credentials;
    }

    private void validateFallback(Long accountId, AccAcountType type) {
        if (accountId != null) {
            accountService.validatePostableAccount(accountId, Set.of(type));
        }
    }

    private void storeCredentials(AccConnectionEntity connection, ConnectorCredentials credentials) {
        var encrypted = credentialCipher.encrypt(credentials);
        connection.setCredentials(encrypted.ciphertext());
        connection.setCredentialsKeyVersion(encrypted.keyVersion());
    }

    private static HashMap<String, String> syncSettingsOf(Map<String, String> syncSettings) {
        return syncSettings == null ? new HashMap<>() : new HashMap<>(syncSettings);
    }

    private static IllegalArgumentException duplicateName(String name) {
        return new IllegalArgumentException("Connection with name " + name + " already exists");
    }

    private void audit(AuditAction action, AccConnectionEntity connection) {
        var tenant = TenantContext.getCurrentTenant();
        var resource = "connection:" + (tenant == null ? "" : tenant.getTenantId() + "/") + connection.getId();
        var details = "type=" + connection.getConnectorType() + ", name=" + connection.getName();
        eventPublisher.publishEvent(AuditEvent.ofCurrentUser(action, resource, details));
    }
}
