package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Synchronization in progress. It is saved in the connection's sync state with every committed page, so a failed
 * sync resumes with the same window and the cursor of the first page not yet staged. A finished run leaves only the
 * data revision of the source in the sync state.
 *
 * @param cursor       connector cursor of the next page, {@code null} for the first one
 * @param dataRevision revision of the source data at the start of the run, {@code null} when the source does not tell
 */
record ImportRun(Instant startedAt, RecordWindow window, String cursor, String dataRevision) {

    private static final String STARTED_AT_KEY = "runStartedAt";
    private static final String WINDOW_FROM_KEY = "windowFrom";
    private static final String WINDOW_TO_KEY = "windowTo";
    private static final String CURSOR_KEY = "cursor";
    private static final String DATA_REVISION_KEY = "dataRevision";

    static ImportRun start(Instant startedAt, RecordWindow window, String dataRevision) {
        return new ImportRun(startedAt, window, null, dataRevision);
    }

    static Optional<ImportRun> fromState(Map<String, String> state) {
        if (state == null || !state.containsKey(STARTED_AT_KEY)) {
            return Optional.empty();
        }
        var window = RecordWindow.between(
                LocalDate.parse(state.get(WINDOW_FROM_KEY)), LocalDate.parse(state.get(WINDOW_TO_KEY)));
        return Optional.of(new ImportRun(Instant.parse(state.get(STARTED_AT_KEY)), window, state.get(CURSOR_KEY),
                state.get(DATA_REVISION_KEY)));
    }

    /**
     * Data revision of the source at the start of the last finished run.
     */
    static Optional<String> lastDataRevision(Map<String, String> state) {
        return state == null ? Optional.empty() : Optional.ofNullable(state.get(DATA_REVISION_KEY));
    }

    ImportRun withCursor(String nextCursor) {
        return new ImportRun(startedAt, window, nextCursor, dataRevision);
    }

    HashMap<String, String> toState() {
        var state = finishedState();
        state.put(STARTED_AT_KEY, startedAt.toString());
        state.put(WINDOW_FROM_KEY, window.from().toString());
        state.put(WINDOW_TO_KEY, window.to().toString());
        if (cursor != null) {
            state.put(CURSOR_KEY, cursor);
        }
        return state;
    }

    /**
     * Sync state once the run is finished: only the data revision, empty when the source does not tell it.
     */
    HashMap<String, String> finishedState() {
        var state = new HashMap<String, String>();
        if (dataRevision != null) {
            state.put(DATA_REVISION_KEY, dataRevision);
        }
        return state;
    }
}
