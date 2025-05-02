package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Resolve the current tenant identifier by UserEntity#schemaId
     */
    @Override
    public Object resolveCurrentTenantIdentifier() {
        var currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            return currentTenant;
        }
        return TenantUtil.getCurrentTenantIdentifier(jdbcTemplate);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }

}
