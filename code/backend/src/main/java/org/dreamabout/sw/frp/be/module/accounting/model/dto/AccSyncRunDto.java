package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;

import java.time.Instant;

/**
 * One synchronization of a connection.
 *
 * @param finishedAt   end of the run, {@code null} while it is running
 * @param created      records staged for the first time
 * @param updated      staged records changed in the source
 * @param deleted      staged records deleted in the source
 * @param errors       records that could not be posted (errors and conflicts)
 * @param errorMessage why the run failed
 */
public record AccSyncRunDto(
    Long id,
    SyncTrigger trigger,
    SyncRunStatus status,
    Instant startedAt,
    Instant finishedAt,
    int fetched,
    int created,
    int updated,
    int deleted,
    int posted,
    int errors,
    String errorMessage
) {}
