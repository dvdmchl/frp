package org.dreamabout.sw.frp.be.module.accounting.domain;

import java.util.Collections;
import java.util.Set;

/**
 * What an external mapping maps and which accounting account types it may target.
 */
public enum ExternalMappingKind {
    /** Account in the source (cash, current, saving, credit card, loan). */
    ACCOUNT(Set.of(AccAcountType.ASSET, AccAcountType.LIABILITY)),
    /** Category in the source; the amount sign decides the direction of the posting. */
    CATEGORY(Set.of(AccAcountType.EXPENSE, AccAcountType.REVENUE));

    private final Set<AccAcountType> targetTypes;

    ExternalMappingKind(Set<AccAcountType> targetTypes) {
        this.targetTypes = targetTypes;
    }

    public Set<AccAcountType> targetTypes() {
        return Collections.unmodifiableSet(targetTypes);
    }
}
