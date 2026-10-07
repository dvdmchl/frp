package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Multitenant
public interface AccConnectionRepository extends JpaRepository<AccConnectionEntity, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Ids of the enabled connections with accepted credentials whose next scheduled synchronization is due.
     */
    @Query("SELECT c.id FROM AccConnectionEntity c WHERE c.enabled = true AND c.credentials IS NOT NULL"
            + " AND c.credentialsRejected = false AND (c.nextSyncAt IS NULL OR c.nextSyncAt <= :now) ORDER BY c.id")
    List<Long> findDueForSync(Instant now);

    /**
     * Takes the synchronization lock of the connection for the current transaction; {@code false} when another
     * transaction holds it. An advisory lock rather than a row lock, so the running sync can still commit its progress
     * to the connection row from its own page transactions.
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(hashtextextended(current_schema() || '.acc_connection.' || :id, 0))",
            nativeQuery = true)
    boolean tryLockForSync(Long id);
}
