package org.dreamabout.sw.frp.be.module.accounting.domain;

/**
 * State of a synchronization run.
 */
public enum SyncRunStatus {
    /** Still running, or interrupted by an application stop before it could finish. */
    RUNNING,
    /** Records fetched and all pending records posted. */
    SUCCESS,
    /** Records fetched, but some records could not be posted (errors or conflicts). */
    PARTIAL,
    /** The run stopped with an error; see the error message. */
    FAILED
}
