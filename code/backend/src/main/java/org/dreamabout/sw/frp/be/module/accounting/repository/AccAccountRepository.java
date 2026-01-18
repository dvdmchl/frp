package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Multitenant
public interface AccAccountRepository extends JpaRepository<AccAccountEntity, Long> {
    Optional<AccAccountEntity> findByName(String name);
    boolean existsByCurrency(AccCurrencyEntity currency);
}
