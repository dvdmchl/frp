package org.dreamabout.sw.frp.be.config.context;

import lombok.experimental.UtilityClass;
import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;

/**
 * Thread-local context for storing and retrieving data.
 * Wraps MultitenancyThreadContext from the library.
 */
@UtilityClass
public class FrpThreadContext {

    public static void set(String key, Object value) {
        MultitenancyThreadContext.set(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return MultitenancyThreadContext.get(key);
    }

    public static void clear() {
        MultitenancyThreadContext.clear();
    }

    public static String getCurrentSearchPath() {
        return MultitenancyThreadContext.getCurrentSearchPath();
    }

    public static void setCurrentSearchPath(String searchPath) {
        MultitenancyThreadContext.setCurrentSearchPath(searchPath);
    }

    public static void setTenantIndentifier(TenantIdentifier tenantIdentifier) {
        MultitenancyThreadContext.setTenantIdentifier(tenantIdentifier);
    }

    public static TenantIdentifier getTenantIdentifier() {
        return MultitenancyThreadContext.getTenantIdentifier();
    }
}

