package org.dreamabout.sw.frp.be.module.accounting.domain;

/**
 * What started a synchronization run.
 */
public enum SyncTrigger {
    /** The scheduler, because the interval of the connection elapsed. */
    SCHEDULED,
    /** A user ("sync now"). */
    MANUAL
}
