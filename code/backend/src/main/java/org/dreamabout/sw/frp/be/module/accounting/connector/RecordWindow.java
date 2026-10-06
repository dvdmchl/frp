package org.dreamabout.sw.frp.be.module.accounting.connector;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Which records to fetch: record dates in {@code [from, to]}, optionally only those changed since a point in time
 * (incremental sync).
 */
@EqualsAndHashCode
@ToString
public final class RecordWindow {

    private final LocalDate from;
    private final LocalDate to;
    private final Instant updatedSince;

    private RecordWindow(LocalDate from, LocalDate to, Instant updatedSince) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Window end " + to + " is before its start " + from);
        }
        this.updatedSince = updatedSince;
    }

    public static RecordWindow between(LocalDate from, LocalDate to) {
        return new RecordWindow(from, to, null);
    }

    public RecordWindow withUpdatedSince(Instant since) {
        return new RecordWindow(from, to, Objects.requireNonNull(since, "since"));
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    public Optional<Instant> updatedSince() {
        return Optional.ofNullable(updatedSince);
    }
}
