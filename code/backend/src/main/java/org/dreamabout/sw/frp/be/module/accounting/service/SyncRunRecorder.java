package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccSyncRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Writes the history of synchronization runs. Every call commits on its own, so a run is visible while it is running
 * and stays recorded when it fails.
 */
@Service
@RequiredArgsConstructor
public class SyncRunRecorder {

    static final String INTERRUPTED = "Interrupted by an application stop";

    private final AccSyncRunRepository syncRunRepository;
    private final AccConnectionRepository connectionRepository;
    private final ConnectorProperties connectorProperties;
    private final Clock clock;

    /**
     * Records a new {@link SyncRunStatus#RUNNING} run. Called under the sync lock of the connection, so runs of the
     * connection still marked as running are leftovers of an application stop and are failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(Long connectionId, SyncTrigger trigger) {
        var now = clock.instant();
        syncRunRepository.failRunning(connectionId, now, INTERRUPTED);
        var run = new AccSyncRunEntity();
        run.setConnectionId(connectionId);
        run.setTrigger(trigger);
        run.setStatus(SyncRunStatus.RUNNING);
        run.setStartedAt(now);
        return syncRunRepository.save(run).getId();
    }

    /**
     * Finishes the run with its outcome, schedules the next synchronization of the connection and deletes the runs
     * beyond the kept history.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccSyncRunEntity finish(Long runId, SyncRunOutcome outcome) {
        var run = syncRunRepository.findById(runId).orElseThrow();
        run.setStatus(outcome.status());
        run.setFinishedAt(clock.instant());
        run.setFetched(outcome.imported().fetched());
        run.setCreated(outcome.imported().created());
        run.setUpdated(outcome.imported().updated());
        run.setDeleted(outcome.imported().deleted());
        run.setPosted(outcome.posted().posted());
        run.setErrors(outcome.posted().failed() + outcome.posted().conflicts());
        run.setErrorMessage(outcome.errorMessage());
        var finished = syncRunRepository.save(run);

        var connection = connectionRepository.findById(run.getConnectionId()).orElseThrow();
        connection.setNextSyncAt(outcome.nextSyncAt());
        if (outcome.credentialsRejected()) {
            connection.setCredentialsRejected(true);
        }
        connectionRepository.save(connection);

        syncRunRepository.deleteAllButLatest(run.getConnectionId(), connectorProperties.getSyncRunsKept());
        return finished;
    }
}
