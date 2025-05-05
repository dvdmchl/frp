package org.dreamabout.sw.frp.be.config.db;

import lombok.experimental.UtilityClass;
import org.dreamabout.sw.frp.be.config.context.FrpThreadContext;

@UtilityClass
public class TenantContext {

    public static void setCurrentTenant(TenantIdentifier tenantIdentifier) {
        FrpThreadContext.setTenantIndentifier(tenantIdentifier);
    }

    public static TenantIdentifier getCurrentTenant() {
        return FrpThreadContext.getTenantIdentifier();
    }

}
