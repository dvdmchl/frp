package org.dreamabout.sw.frp.be.module.accounting.model.dto;

import java.util.List;

/**
 * Connector type available for new connections and the credentials a connection to it needs.
 */
public record AccConnectorDto(
    String type,
    List<AccCredentialFieldDto> credentialFields
) {
    public AccConnectorDto {
        credentialFields = List.copyOf(credentialFields);
    }
}
