package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
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

    List<AccImportRecordEntity> findByConnectionIdAndStatusIn(Long connectionId, Collection<ImportRecordStatus> statuses);

    /**
     * Net amount of the connection's records per external category, as rows of category id and sum.
     */
    @Query("""
            SELECT r.externalCategoryId, SUM(r.amount) FROM AccImportRecordEntity r
            WHERE r.connectionId = :connectionId AND r.externalCategoryId IS NOT NULL
            GROUP BY r.externalCategoryId""")
    List<Object[]> sumAmountByCategory(Long connectionId);

    /**
     * Returns the skipped records of the external account to {@link ImportRecordStatus#NEW}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AccImportRecordEntity r
            SET r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.NEW,
                r.version = r.version + 1
            WHERE r.connectionId = :connectionId AND r.externalAccountId = :externalId
              AND r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.SKIPPED""")
    int reopenSkippedOfAccount(Long connectionId, String externalId);

    /**
     * Returns the skipped records of the external category to {@link ImportRecordStatus#NEW}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AccImportRecordEntity r
            SET r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.NEW,
                r.version = r.version + 1
            WHERE r.connectionId = :connectionId AND r.externalCategoryId = :externalId
              AND r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.SKIPPED""")
    int reopenSkippedOfCategory(Long connectionId, String externalId);

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

    List<AccImportRecordEntity> findByConnectionIdAndTransferLinkIdAndIdNot(Long connectionId, String transferLinkId,
                                                                         Long id);

    List<AccImportRecordEntity> findByTransactionId(Long transactionId);

    /**
     * Ids of the connection's records deleted in the source whose transaction has not been deleted yet.
     */
    @Query("""
            SELECT r.id FROM AccImportRecordEntity r
            WHERE r.connectionId = :connectionId AND r.transactionId IS NOT NULL
              AND r.status = org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus.DELETED
            ORDER BY r.id""")
    List<Long> findDeletedWithTransaction(Long connectionId);
}
