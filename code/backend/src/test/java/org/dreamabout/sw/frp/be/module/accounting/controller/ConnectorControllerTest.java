package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectorDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCredentialFieldDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConnectorControllerTest extends AbstractConnectionApiTest {

    @Test
    void shouldListConnectorsWithTheirCredentialFields() throws Exception {
        when(connectionService.getConnectors()).thenReturn(
                List.of(new AccConnectorDto("WALLET", List.of(new AccCredentialFieldDto("token", true)))));

        perform(get(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTORS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WALLET"))
                .andExpect(jsonPath("$[0].credentialFields[0].name").value("token"))
                .andExpect(jsonPath("$[0].credentialFields[0].secret").value(true));
    }
}
