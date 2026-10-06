package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCopiedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCreatedEvent;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaDroppedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes FRP's {@link AuditEvent}s and the tenant schema lifecycle events of spring-pg-multitenancy into the audit log.
 * Events are recorded only after the publishing transaction commits (or immediately without a transaction),
 * so rolled back actions are not logged. A failure to write the log never breaks the audited action.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditLogService auditLogService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onAuditEvent(AuditEvent event) {
        recordSafely(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSchemaCreated(TenantSchemaCreatedEvent event) {
        recordSafely(AuditEvent.ofCurrentUser(AuditAction.SCHEMA_CREATED,
                AuditEvent.schemaResource(event.schemaName()), null));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSchemaCopied(TenantSchemaCopiedEvent event) {
        recordSafely(AuditEvent.ofCurrentUser(AuditAction.SCHEMA_COPIED,
                AuditEvent.schemaResource(event.targetSchemaName()), "source=" + event.sourceSchemaName()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSchemaDropped(TenantSchemaDroppedEvent event) {
        recordSafely(AuditEvent.ofCurrentUser(AuditAction.SCHEMA_DROPPED,
                AuditEvent.schemaResource(event.schemaName()), null));
    }

    private void recordSafely(AuditEvent event) {
        try {
            auditLogService.recordEvent(event);
        } catch (RuntimeException e) {
            log.error("Failed to write audit log entry {}", event, e);
        }
    }
}
