package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.dreamabout.sw.frp.be.domain.Constant.PUBLIC_SCHEMA;

@Component
@RequiredArgsConstructor
public class MultiSchemaConnectionProvider implements MultiTenantConnectionProvider<TenantIdentifier>, HibernatePropertiesCustomizer {

    private final transient DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return getConnection(TenantIdentifier.of(PUBLIC_SCHEMA));
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(TenantIdentifier tenantIdentifier) throws SQLException {
        if (tenantIdentifier == null) {
            throw new IllegalArgumentException("Tenant identifier is null");
        }

        var connection = dataSource.getConnection();
        try {
            connection.setSchema(tenantIdentifier.getTenantId());
        }
        catch (SQLException e) {
            connection.close();
            throw new SQLException("Failed to set schema: " + tenantIdentifier.getTenantId(), e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(TenantIdentifier o, Connection connection) throws SQLException {
        connection.setSchema(PUBLIC_SCHEMA);
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isInstance(this)
                || MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (unwrapType.isInstance(this)) {
            return (T) this;
        }
        if (MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return (T) this;
        }
        throw new UnknownUnwrapTypeException(unwrapType);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
