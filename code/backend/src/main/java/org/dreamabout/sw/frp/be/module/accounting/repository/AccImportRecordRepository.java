package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Multitenant
public interface AccImportRecordRepository extends JpaRepository<AccImportRecordEntity, Long> {

    Optional<AccImportRecordEntity> findByConnectionIdAndExternalId(Long connectionId, String externalId);

    List<AccImportRecordEntity> findByConnectionIdAndExternalIdIn(Long connectionId, Collection<String> externalIds);

    /**
     * Notes that the records were fetched again; nothing else changes (status and version stay).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE AccImportRecordEntity r SET r.lastSeenAt = :seenAt WHERE r.id IN :ids")
    void markSeen(Collection<Long> ids, Instant seenAt);

    /**
     * Marks records of the connection dated in {@code [from, to]} that were not fetched since {@code seenSince} as
     * deleted.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AccImportRecordEntity r
            SET r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.DELETED,
                r.errorMessage = NULL, r.version = r.version + 1
            WHERE r.connectionId = :connectionId
              AND r.status <> org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.DELETED
              AND r.recordDate BETWEEN :from AND :to
              AND r.lastSeenAt < :seenSince""")
    int markNotSeenAsDeleted(Long connectionId, LocalDate from, LocalDate to, Instant seenSince);
}
