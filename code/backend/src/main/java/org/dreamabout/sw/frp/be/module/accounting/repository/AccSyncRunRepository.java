package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Multitenant
public interface AccSyncRunRepository extends JpaRepository<AccSyncRunEntity, Long> {

    List<AccSyncRunEntity> findByConnectionIdOrderByStartedAtDescIdDesc(Long connectionId);

    /**
     * Fails the runs of the connection that are still {@code RUNNING}; called under the sync lock, so they can only be
     * leftovers of an application stop.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE AccSyncRunEntity r SET r.status = org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus.FAILED,"
            + " r.finishedAt = :finishedAt, r.errorMessage = :errorMessage"
            + " WHERE r.connectionId = :connectionId"
            + " AND r.status = org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus.RUNNING")
    int failRunning(Long connectionId, Instant finishedAt, String errorMessage);

    /**
     * Deletes all runs of the connection except the {@code kept} most recent ones.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM acc_sync_run WHERE connection_id = :connectionId AND id NOT IN ("
            + "SELECT id FROM acc_sync_run WHERE connection_id = :connectionId"
            + " ORDER BY started_at DESC, id DESC LIMIT :kept)", nativeQuery = true)
    int deleteAllButLatest(Long connectionId, int kept);
}
