package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorNotReadyException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccSyncRunDto;
import org.dreamabout.sw.frp.be.module.accounting.model.mapper.SyncRunMapper;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccSyncRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Runs a synchronization of a connection: fetches its records into the staging, posts them and records the run in the
 * history of the connection together with the time of its next scheduled synchronization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final AccConnectionRepository connectionRepository;
    private final ConnectionService connectionService;
    private final ImportService importService;
    private final ImportPostingService postingService;
    private final SyncRunRecorder runRecorder;
    private final AccSyncRunRepository syncRunRepository;
    private final SyncRunMapper syncRunMapper;
    private final ConnectorProperties connectorProperties;
    private final Clock clock;

    /**
     * Synchronizes the connection. The transaction only holds the sync lock of the connection for the whole run
     * (fetching and posting); the work commits in its own transactions. A failed run is recorded and its exception
     * rethrown: a rate limited source is retried after its delay, a source that is not ready after
     * {@code frp.connector.sync-not-ready-retry}, and rejected credentials pause the schedule until new ones are set.
     */
    @Transactional
    public AccSyncRunEntity sync(Long connectionId, SyncTrigger trigger) {
        importService.lockForSync(connectionId);
        var connection = connectionService.findConnection(connectionId);
        Long runId = runRecorder.start(connectionId, trigger);
        try {
            var imported = importService.sync(connectionId);
            var posted = postingService.post(connectionId);
            return runRecorder.finish(runId, SyncRunOutcome.completed(imported, posted, nextRegularSync(connection)));
        } catch (RuntimeException e) {
            log.warn("Synchronization of connection {} failed: {}", connectionId, e.getMessage());
            runRecorder.finish(runId, failed(connection, e));
            throw e;
        }
    }

    /**
     * History of the connection's synchronizations, the latest first.
     */
    @Transactional(readOnly = true)
    public List<AccSyncRunDto> getRuns(Long connectionId) {
        connectionService.findConnection(connectionId);
        return syncRunRepository.findByConnectionIdOrderByStartedAtDescIdDesc(connectionId).stream()
                .map(syncRunMapper::toDto)
                .toList();
    }

    /**
     * Ids of the connections of the current tenant whose scheduled synchronization is due.
     */
    @Transactional(readOnly = true)
    public List<Long> findDueForSync() {
        return connectionRepository.findDueForSync(clock.instant());
    }

    private SyncRunOutcome failed(AccConnectionEntity connection, RuntimeException e) {
        var message = ExceptionMessages.of(e);
        return switch (e) {
            case ConnectorAuthException _ -> SyncRunOutcome.failed(message, nextRegularSync(connection), true);
            case ConnectorRateLimitedException limited ->
                    SyncRunOutcome.failed(message, clock.instant().plus(limited.retryAfter()), false);
            case ConnectorNotReadyException _ ->
                    SyncRunOutcome.failed(message, clock.instant().plus(connectorProperties.getSyncNotReadyRetry()), false);
            default -> SyncRunOutcome.failed(message, nextRegularSync(connection), false);
        };
    }

    private Instant nextRegularSync(AccConnectionEntity connection) {
        return clock.instant().plus(Duration.ofMinutes(connection.getSyncIntervalMinutes()));
    }
}
