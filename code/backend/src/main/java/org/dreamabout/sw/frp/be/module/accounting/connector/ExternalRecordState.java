package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * Source-neutral state of a record.
 */
public enum ExternalRecordState {
    /** Settled record. */
    BOOKED,
    /** Record the source has not settled yet (e.g. a pending card payment). */
    PENDING,
    /** Record voided or deleted in the source. */
    DELETED
}
