package org.dreamabout.sw.frp.be.module.accounting.model.dto;

/**
 * External account or category of a connection and the accounting account it is mapped to.
 *
 * @param externalName name in the source; {@code null} when the source no longer lists it
 * @param accountName  name of the mapped accounting account; {@code null} when unmapped
 */
public record AccMappedItemDto(
    String externalId,
    String externalName,
    String accountName
) {}
