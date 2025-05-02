package org.dreamabout.sw.frp.be.config.db;

public interface TenantIdentifier {
    String getTenantId();

    static TenantIdentifier of(String tenantId) {
        return () -> tenantId;
    }
}
