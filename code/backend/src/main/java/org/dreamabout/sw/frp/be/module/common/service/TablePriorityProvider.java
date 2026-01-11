package org.dreamabout.sw.frp.be.module.common.service;

public interface TablePriorityProvider {
    /**
     * Returns the priority for the given table.
     * Lower value means higher priority (copied earlier).
     * Returns null if this provider doesn't handle the table.
     */
    Integer getTablePriority(String tableName);
}
