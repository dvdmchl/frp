package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaDroppedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogListenerTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditLogListener listener = new AuditLogListener(auditLogService);

    @Test
    void schemaDropped_isRecordedForCurrentUser() {
        listener.onSchemaDropped(new TenantSchemaDroppedEvent("tenant_a"));

        verify(auditLogService).record(new AuditEvent(AuditAction.SCHEMA_DROPPED, null, null, "schema:tenant_a", null));
    }

    @Test
    void failureToRecord_doesNotBreakAuditedAction() {
        doThrow(new DataAccessResourceFailureException("db down")).when(auditLogService).record(any());
        var event = AuditEvent.ofUser(AuditAction.LOGIN, 1L, "a@b.c", null);

        assertThatCode(() -> listener.onAuditEvent(event)).doesNotThrowAnyException();
        verify(auditLogService).record(event);
    }
}
