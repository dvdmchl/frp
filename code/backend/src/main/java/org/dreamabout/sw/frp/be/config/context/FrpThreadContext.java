package org.dreamabout.sw.frp.be.config.context;

import lombok.experimental.UtilityClass;
import org.dreamabout.sw.frp.be.config.db.TenantIdentifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local context for storing and retrieving data.
 */
@UtilityClass
public class FrpThreadContext {

    private static final Map<Long, Map<String, Object>> STORAGE = new ConcurrentHashMap<>();

    public static void init() {
        STORAGE.computeIfAbsent(Thread.currentThread().threadId(), id -> new ConcurrentHashMap<>());
    }

    public static void set(String key, Object value) {
        init();
        STORAGE.get(Thread.currentThread().threadId()).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Map<String, Object> ctx = STORAGE.get(Thread.currentThread().threadId());
        return ctx != null ? (T) ctx.get(key) : null;
    }

    public static void clear() {
        STORAGE.remove(Thread.currentThread().threadId());
    }

    public static String getCurrentSearchPath() {
        return get("currentSearchPath");
    }

    public static void setCurrentSearchPath(String searchPath) {
        set("currentSearchPath", searchPath);
    }

    public static void setTenantIndentifier(TenantIdentifier tenantIdentifier) {
        set("tenantIdentifier", tenantIdentifier);
    }

    public static TenantIdentifier getTenantIdentifier() {
        return get("tenantIdentifier");
    }
}
