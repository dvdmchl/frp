package org.dreamabout.sw.frp.be.module.accounting.service;

/**
 * What posting did with one staged record.
 */
public enum PostingOutcome {
    /** Posted as a new transaction or rewrote the journals of its transaction. */
    POSTED,
    /** The record was deleted in the source, so its transaction was deleted. */
    REMOVED,
    /** The transaction was changed in FRP, so the record became a conflict. */
    CONFLICT,
    /** Posting failed and the record was marked as an error. */
    FAILED,
    /** The other leg of the transfer is not staged or not mapped yet. */
    WAITING,
    /** Nothing to do any more, e.g. the record was posted together with the other leg of its transfer. */
    UNCHANGED
}
