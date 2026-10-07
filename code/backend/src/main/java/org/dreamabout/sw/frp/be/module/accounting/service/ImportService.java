package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.AccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorCredentials;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRegistry;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordPage;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

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

    /**
     * Key of the import start date ({@code yyyy-MM-dd}) in the sync settings of a connection.
     */
    static final String IMPORT_START_DATE_SETTING = "importStartDate";

    private final AccConnectionRepository connectionRepository;
    private final ConnectionService connectionService;
    private final ConnectorRegistry connectorRegistry;
    private final ImportStagingService stagingService;
    private final ConnectorProperties connectorProperties;
    private final Clock clock;

    /**
     * Synchronizes the connection: resumes an unfinished run, otherwise starts a new one unless the source reports the
     * same data revision as at the start of the last finished run.
     */
    @Transactional
    public ImportResult sync(Long connectionId) {
        takeSyncLock(connectionId);
        var connection = connectionService.findEnabledConnection(connectionId);
        var connector = connectorRegistry.get(connection.getConnectorType());
        var credentials = connectionService.credentialsOf(connection);
        var resumed = ImportRun.fromState(connection.getSyncState());
        if (resumed.isPresent()) {
            return fetch(connectionId, connector, credentials, resumed.get());
        }
        var revision = connector.dataRevision(credentials);
        if (revision.isPresent() && revision.equals(ImportRun.lastDataRevision(connection.getSyncState()))) {
            stagingService.finishUnchanged(connectionId);
            log.info("Synchronized connection {}: source data unchanged since the last sync", connectionId);
            return ImportResult.EMPTY;
        }
        return fetch(connectionId, connector, credentials, startRun(connection, revision.orElse(null)));
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

    private ImportResult fetch(Long connectionId, AccountingConnector connector, ConnectorCredentials credentials,
                               ImportRun firstPageRun) {
        var run = firstPageRun;
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

    private ImportRun startRun(AccConnectionEntity connection, String dataRevision) {
        var today = LocalDate.now(clock);
        var window = RecordWindow.between(windowStart(connection, today), today);
        return ImportRun.start(clock.instant(), window, dataRevision);
    }

    /**
     * Start of the window: {@code frp.connector.sync-window-days} back from today, but never before the import start
     * date of the connection. The first sync of a connection starts at the import start date, so it imports the
     * history from then on.
     */
    private LocalDate windowStart(AccConnectionEntity connection, LocalDate today) {
        var regularStart = today.minusDays(connectorProperties.getSyncWindowDays());
        var importStart = Optional.ofNullable(connection.getSyncSettings().get(IMPORT_START_DATE_SETTING))
                .map(LocalDate::parse);
        if (importStart.isEmpty()) {
            return regularStart;
        }
        var start = connection.getLastSuccessfulSyncAt() == null
                ? importStart.get() : max(regularStart, importStart.get());
        return min(start, today);
    }

    private static LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }
}
