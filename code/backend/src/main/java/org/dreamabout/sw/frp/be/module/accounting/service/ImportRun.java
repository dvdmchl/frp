package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Synchronization in progress. It is saved in the connection's sync state with every committed page, so a failed
 * sync resumes with the same window and the cursor of the first page not yet staged.
 *
 * @param cursor connector cursor of the next page, {@code null} for the first one
 */
record ImportRun(Instant startedAt, RecordWindow window, String cursor) {

    private static final String STARTED_AT_KEY = "runStartedAt";
    private static final String WINDOW_FROM_KEY = "windowFrom";
    private static final String WINDOW_TO_KEY = "windowTo";
    private static final String CURSOR_KEY = "cursor";

    static ImportRun start(Instant startedAt, RecordWindow window) {
        return new ImportRun(startedAt, window, null);
    }

    static Optional<ImportRun> fromState(Map<String, String> state) {
        if (state == null || !state.containsKey(STARTED_AT_KEY)) {
            return Optional.empty();
        }
        var window = RecordWindow.between(
                LocalDate.parse(state.get(WINDOW_FROM_KEY)), LocalDate.parse(state.get(WINDOW_TO_KEY)));
        return Optional.of(new ImportRun(Instant.parse(state.get(STARTED_AT_KEY)), window, state.get(CURSOR_KEY)));
    }

    ImportRun withCursor(String nextCursor) {
        return new ImportRun(startedAt, window, nextCursor);
    }

    HashMap<String, String> toState() {
        var state = new HashMap<String, String>();
        state.put(STARTED_AT_KEY, startedAt.toString());
        state.put(WINDOW_FROM_KEY, window.from().toString());
        state.put(WINDOW_TO_KEY, window.to().toString());
        if (cursor != null) {
            state.put(CURSOR_KEY, cursor);
        }
        return state;
    }
}
