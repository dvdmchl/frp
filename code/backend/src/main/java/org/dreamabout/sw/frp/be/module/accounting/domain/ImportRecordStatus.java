package org.dreamabout.sw.frp.be.module.accounting.domain;

/**
 * Processing state of a staged external record.
 */
public enum ImportRecordStatus {
    /** New or changed in the source; waits to be posted. */
    NEW,
    /** Posted as an accounting transaction. */
    POSTED,
    /** Intentionally not posted. */
    SKIPPED,
    /** Posting failed; see the error message. */
    ERROR,
    /** The posted transaction was changed in FRP and conflicts with the source; the user has to resolve it. */
    CONFLICT,
    /** Deleted or voided in the source, or no longer returned by it. */
    DELETED
}
