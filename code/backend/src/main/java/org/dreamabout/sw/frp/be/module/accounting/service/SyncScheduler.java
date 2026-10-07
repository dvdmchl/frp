package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.common.service.FrpTenantRegistry;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Synchronizes the connections of all tenants whose interval elapsed. Switched off by
 * {@code frp.connector.scheduled-sync-enabled=false}; {@code frp.connector.sync-check-interval} sets how often it looks
 * for due connections.
 */
@Component
@ConditionalOnProperty(name = "frp.connector.scheduled-sync-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final FrpTenantRegistry tenantRegistry;
    private final SyncService syncService;

    @Scheduled(fixedDelayString = "${frp.connector.sync-check-interval:PT5M}",
            initialDelayString = "${frp.connector.sync-check-interval:PT5M}")
    public void syncDueConnections() {
        for (String schema : tenantRegistry.getTenantSchemas()) {
            TenantContext.setCurrentTenant(TenantIdentifier.of(schema));
            try {
                syncService.findDueForSync().forEach(connectionId -> syncScheduled(schema, connectionId));
            } catch (RuntimeException e) {
                log.error("Scheduled synchronization of schema {} failed", schema, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void syncScheduled(String schema, Long connectionId) {
        try {
            syncService.sync(connectionId, SyncTrigger.SCHEDULED);
        } catch (RuntimeException e) {
            // recorded in the run history (or the connection is being synchronized already); go on with the others
            log.debug("Scheduled synchronization of connection {} in schema {} failed", connectionId, schema, e);
        }
    }
}
