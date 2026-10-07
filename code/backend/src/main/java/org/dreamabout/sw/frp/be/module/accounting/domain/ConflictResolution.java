package org.dreamabout.sw.frp.be.module.accounting.domain;

/**
 * How a staged record in conflict with a transaction changed or deleted in FRP is resolved.
 */
public enum ConflictResolution {
    /** Keep the transaction as it is in FRP; the record no longer changes it. */
    KEEP_FRP,
    /** Apply the record from the source: overwrite, recreate or delete the transaction. */
    USE_SOURCE
}
