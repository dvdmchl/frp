package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRegistry;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Synchronizes a connection: fetches the records of its window from the source page by page into the import staging.
 *
 * <p>Every page is committed in its own transaction together with the connector cursor of the next page, so a failed
 * sync resumes after the last committed page without duplicates. The transaction of {@link #sync} only holds the
 * lock that keeps a second sync of the same connection out.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final AccConnectionRepository connectionRepository;
    private final ConnectionService connectionService;
    private final ConnectorRegistry connectorRegistry;
    private final ImportStagingService stagingService;
    private final ConnectorProperties connectorProperties;
    private final Clock clock;

    @Transactional
    public ImportResult sync(Long connectionId) {
        takeSyncLock(connectionId);
        var connection = connectionService.findEnabledConnection(connectionId);
        var connector = connectorRegistry.get(connection.getConnectorType());
        var credentials = connectionService.credentialsOf(connection);
        var run = ImportRun.fromState(connection.getSyncState()).orElseGet(this::startRun);

        var result = ImportResult.EMPTY;
        RecordPage page;
        do {
            page = connector.fetchRecords(credentials, run.window(), run.cursor());
            result = result.plus(stagingService.stagePage(connectionId, run, page));
            run = run.withCursor(page.nextCursor());
        } while (!page.isLast());

        log.info("Synchronized connection {}: {}", connectionId, result);
        return result;
    }

    /**
     * Takes the sync lock of the connection until the end of the current transaction; the lock is reentrant within it.
     *
     * @throws IllegalStateException when another transaction synchronizes the connection
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForSync(Long connectionId) {
        takeSyncLock(connectionId);
    }

    private void takeSyncLock(Long connectionId) {
        if (!connectionRepository.tryLockForSync(connectionId)) {
            throw new IllegalStateException("Synchronization of connection " + connectionId + " is already running");
        }
    }

    private ImportRun startRun() {
        var today = LocalDate.now(clock);
        var window = RecordWindow.between(today.minusDays(connectorProperties.getSyncWindowDays()), today);
        return ImportRun.start(clock.instant(), window);
    }
}
