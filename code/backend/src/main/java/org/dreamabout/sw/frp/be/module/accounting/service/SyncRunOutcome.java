package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;

import java.time.Instant;

/**
 * How a synchronization run ended and when the connection is synchronized next.
 *
 * @param credentialsRejected the source rejected the credentials; the schedule of the connection is paused
 */
record SyncRunOutcome(SyncRunStatus status, ImportResult imported, PostingResult posted, String errorMessage,
                      Instant nextSyncAt, boolean credentialsRejected) {

    /**
     * Records fetched and posted; {@link SyncRunStatus#PARTIAL} when some records failed or are in conflict.
     */
    static SyncRunOutcome completed(ImportResult imported, PostingResult posted, Instant nextSyncAt) {
        var status = posted.failed() + posted.conflicts() > 0 ? SyncRunStatus.PARTIAL : SyncRunStatus.SUCCESS;
        return new SyncRunOutcome(status, imported, posted, null, nextSyncAt, false);
    }

    static SyncRunOutcome failed(String errorMessage, Instant nextSyncAt, boolean credentialsRejected) {
        return new SyncRunOutcome(SyncRunStatus.FAILED, ImportResult.EMPTY, PostingResult.EMPTY, errorMessage,
                nextSyncAt, credentialsRejected);
    }
}
