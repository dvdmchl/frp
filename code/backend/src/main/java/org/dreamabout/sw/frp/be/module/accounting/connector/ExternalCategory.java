package org.dreamabout.sw.frp.be.module.accounting.connector;

/**
 * Category as the source knows it.
 *
 * @param parentExternalId id of the parent category, {@code null} for a top-level one
 * @param rawPayload       the source representation (JSON)
 */
public record ExternalCategory(String externalId, String name, String parentExternalId, String rawPayload) {
}
