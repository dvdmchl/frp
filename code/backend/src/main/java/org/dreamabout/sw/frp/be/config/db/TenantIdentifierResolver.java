package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<TenantIdentifier>, HibernatePropertiesCustomizer {

    private final TenantUtil tenantUtil;

    /**
     * Resolve the current tenant identifier by UserEntity#schemaId
     */
    @Override
    public TenantIdentifier resolveCurrentTenantIdentifier() {
        var currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            return currentTenant;
        }
        return tenantUtil.getCurrentTenantIdentifier();
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
