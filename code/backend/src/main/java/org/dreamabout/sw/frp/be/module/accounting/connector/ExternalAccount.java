package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * Account as the source knows it.
 *
 * @param rawPayload the source representation (JSON), kept for troubleshooting and later re-mapping
 */
public record ExternalAccount(String externalId, String name, String currencyCode, String rawPayload) {
}
