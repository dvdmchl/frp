package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import jakarta.validation.constraints.NotNull;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;

/**
 * Request to create and map an account for every unmapped external account or category of the kind.
 *
 * @param parentNodeId node to create the accounts under; {@code null} creates them at the top level
 */
public record AccCreateMissingAccountsRequestDto(
    @NotNull ExternalMappingKind kind,
    Long parentNodeId
) {}
