package org.dreamabout.sw.frp.be.module.accounting.service;

/**
 * Counts of one synchronization: records fetched from the source, newly staged, changed, and marked deleted because
 * the source no longer returns them.
 */
public record ImportResult(int fetched, int created, int updated, int deleted) {

    public static final ImportResult EMPTY = new ImportResult(0, 0, 0, 0);

    public ImportResult plus(ImportResult other) {
        return new ImportResult(fetched + other.fetched, created + other.created, updated + other.updated,
                deleted + other.deleted);
    }
}
