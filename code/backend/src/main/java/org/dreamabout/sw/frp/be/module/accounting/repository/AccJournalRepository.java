package org.dreamabout.sw.frp.be.module.accounting.repository;

import java.util.List;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@Multitenant
public interface AccJournalRepository extends JpaRepository<AccJournalEntity, Long> {

    @Query("SELECT j.account.id, SUM(j.credit), SUM(j.debit) FROM AccJournalEntity j GROUP BY j.account.id")
    List<Object[]> findBalances();

    @Query("SELECT SUM(j.credit), SUM(j.debit) FROM AccJournalEntity j WHERE j.account.id = :accountId")
    Object[] findBalanceByAccountId(Long accountId);
}
