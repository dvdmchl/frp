package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.hibernate.TenantIdentifierResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * "Sync now": synchronizes a connection on request of the user in the background.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualSyncService {

    private final SyncService syncService;
    private final ConnectionService connectionService;
    private final SecurityContextService securityContextService;
    private final ThreadPoolTaskExecutor frpExecutor;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    /**
     * Starts the synchronization on the executor (whose {@code MultitenancyTaskDecorator} clears the tenant context
     * after the task) in the tenant and as the user of the caller. The tenant is resolved like Hibernate does it, since
     * HTTP requests carry it only in the signed-in user, not in the {@link TenantContext}. Fails right away when the connection does not exist
     * or is disabled; the returned future completes with the finished run.
     */
    public CompletableFuture<AccSyncRunEntity> syncNow(Long connectionId) {
        connectionService.findEnabledConnection(connectionId);
        var tenant = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
        var authentication = securityContextService.getAuthentication();
        return CompletableFuture.supplyAsync(() -> {
            TenantContext.setCurrentTenant(tenant);
            securityContextService.setAuthentication(authentication);
            try {
                return syncService.sync(connectionId, SyncTrigger.MANUAL);
            } finally {
                securityContextService.clearContext();
            }
        }, frpExecutor).whenComplete((run, failure) -> {
            if (failure != null) {
                log.warn("Manual synchronization of connection {} failed: {}", connectionId, failure.getMessage());
            }
        });
    }
}
