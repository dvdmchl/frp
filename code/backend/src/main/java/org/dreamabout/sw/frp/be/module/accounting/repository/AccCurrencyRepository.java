package org.dreamabout.sw.frp.be.module.accounting.repository;

import org.dreamabout.sw.frp.be.module.accounting.model.AccCurrencyEntity;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Multitenant
public interface AccCurrencyRepository extends JpaRepository<AccCurrencyEntity, Long> {
    Optional<AccCurrencyEntity> findByCode(String code);
}
