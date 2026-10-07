package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCreateMissingAccountsRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExternalMappingControllerTest extends AbstractConnectionApiTest {

    private static final String MAPPINGS = CONNECTIONS + "/1/mappings";
    private static final AccExternalMappingDto CASH = new AccExternalMappingDto(3L, ExternalMappingKind.ACCOUNT,
            "acc-cash", "Cash", "CZK", 10L, false);

    @Test
    void shouldListMappingsOfConnection() throws Exception {
        when(mappingService.getMappings(1L)).thenReturn(List.of(CASH));

        perform(get(MAPPINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalName").value("Cash"))
                .andExpect(jsonPath("$[0].accountId").value(10));
    }

    @Test
    void shouldUpdateMappingOfConnection() throws Exception {
        var request = new AccExternalMappingUpdateRequestDto(10L, false);
        when(mappingService.updateMapping(1L, 3L, request)).thenReturn(CASH);

        perform(put(MAPPINGS + "/3"), request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void shouldRefreshMappingsFromSource() throws Exception {
        when(mappingService.refreshMappings(1L)).thenReturn(List.of(CASH));

        perform(post(MAPPINGS + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("acc-cash"));
    }

    @Test
    void shouldCreateMissingAccounts() throws Exception {
        when(mappingService.createMissingAccounts(1L, ExternalMappingKind.CATEGORY, 2L)).thenReturn(List.of(CASH));

        perform(post(MAPPINGS + "/create-missing-accounts"),
                new AccCreateMissingAccountsRequestDto(ExternalMappingKind.CATEGORY, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRejectCreatingMissingAccountsWithoutKind() throws Exception {
        perform(post(MAPPINGS + "/create-missing-accounts"), new AccCreateMissingAccountsRequestDto(null, 2L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mappingService);
    }
}
