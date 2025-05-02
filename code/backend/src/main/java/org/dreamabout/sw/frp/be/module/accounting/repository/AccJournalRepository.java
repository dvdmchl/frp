package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccJournalRepository extends JpaRepository<AccJournalEntity, Long> {
}
