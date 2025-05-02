package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.dreamabout.sw.frp.be.domain.Constant.PUBLIC_SCHEMA;

@Component
@RequiredArgsConstructor
public class MultiSchemaConnectionProvider implements MultiTenantConnectionProvider, HibernatePropertiesCustomizer {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return getConnection(TenantIdentifier.of(PUBLIC_SCHEMA));
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object o) throws SQLException {
        if (o == null) {
            throw new IllegalArgumentException("Tenant identifier is null");
        }
        if (o instanceof TenantIdentifier tenantIdentifier) {
            var connection = dataSource.getConnection();
            connection.setSchema(tenantIdentifier.getTenantId());
            return connection;
        }
        throw new IllegalArgumentException("Tenant identifier not found. Got type: %s instead of TenantIdentifier".formatted(o.getClass().getName()));
    }

    @Override
    public void releaseConnection(Object o, Connection connection) throws SQLException {
        connection.setSchema(PUBLIC_SCHEMA);
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
