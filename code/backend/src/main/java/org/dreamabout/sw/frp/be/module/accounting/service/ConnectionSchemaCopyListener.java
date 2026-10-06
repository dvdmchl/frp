package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.multitenancy.schema.SchemaNames;
import org.dreamabout.sw.multitenancy.schema.event.TenantSchemaCopiedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * A copied schema must not sync from the sources of the original, so its connections are disabled and lose their
 * credentials. Runs synchronously inside the copy transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionSchemaCopyListener {

    private final JdbcTemplate jdbcTemplate;

    @EventListener
    public void onSchemaCopied(TenantSchemaCopiedEvent event) {
        int disabled = jdbcTemplate.update("UPDATE " + SchemaNames.quote(event.targetSchemaName()) + ".acc_connection"
                + " SET enabled = FALSE, credentials = NULL, credentials_key_version = NULL");
        log.info("Disabled {} connection(s) without credentials in copied schema {}", disabled, event.targetSchemaName());
    }
}
