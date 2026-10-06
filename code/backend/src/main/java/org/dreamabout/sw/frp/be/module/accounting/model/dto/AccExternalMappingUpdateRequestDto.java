package org.dreamabout.sw.frp.be.module.accounting.model.dto;

/**
 * @param accountId accounting account to map to, {@code null} to remove the mapping
 * @param ignored   whether the records of the external account or category are skipped
 */
public record AccExternalMappingUpdateRequestDto(
    Long accountId,
    boolean ignored
) {}
