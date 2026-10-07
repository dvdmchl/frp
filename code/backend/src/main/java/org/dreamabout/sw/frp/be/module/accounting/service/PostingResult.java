package org.dreamabout.sw.frp.be.module.accounting.service;

/**
 * Counts of one posting run: records posted, transactions removed because their record was deleted in the source,
 * conflicts with changes made in FRP and failed records.
 */
public record PostingResult(int posted, int removed, int conflicts, int failed) {

    public static final PostingResult EMPTY = new PostingResult(0, 0, 0, 0);

    public PostingResult plus(PostingOutcome outcome) {
        return switch (outcome) {
            case POSTED -> new PostingResult(posted + 1, removed, conflicts, failed);
            case REMOVED -> new PostingResult(posted, removed + 1, conflicts, failed);
            case CONFLICT -> new PostingResult(posted, removed, conflicts + 1, failed);
            case FAILED -> new PostingResult(posted, removed, conflicts, failed + 1);
            case WAITING, UNCHANGED -> this;
        };
    }
}
