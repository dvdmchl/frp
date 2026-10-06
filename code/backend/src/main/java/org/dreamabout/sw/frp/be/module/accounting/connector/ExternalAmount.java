package org.dreamabout.sw.frp.be.module.accounting.connector;

import java.math.BigDecimal;

/**
 * Signed amount in the currency of the source: negative = money going out of the account, positive = coming in.
 */
public record ExternalAmount(BigDecimal value, String currencyCode) {
}
