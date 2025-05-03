package org.dreamabout.sw.frp.be.config.db;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantContext {

    private static final ThreadLocal<TenantIdentifier> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(TenantIdentifier tenantIdentifier) {
        currentTenant.set(tenantIdentifier);
    }

    public static TenantIdentifier getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
