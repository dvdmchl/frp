package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;

/**
 * External account or category of a connection and the accounting account it is mapped to.
 *
 * @param currencyCode currency of an external account, {@code null} for a category
 * @param accountId    mapped accounting account, {@code null} when unmapped
 */
public record AccExternalMappingDto(
    Long id,
    ExternalMappingKind kind,
    String externalId,
    String externalName,
    String currencyCode,
    Long accountId,
    boolean ignored
) {}
