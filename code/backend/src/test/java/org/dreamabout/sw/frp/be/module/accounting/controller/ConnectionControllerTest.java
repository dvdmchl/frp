package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorTransientException;
import org.dreamabout.sw.frp.be.module.accounting.connector.UnknownConnectorTypeException;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionEnabledRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionFallbackRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccSyncRunDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConnectionControllerTest extends AbstractConnectionApiTest {

    private static final Map<String, String> CREDENTIALS = Map.of("token", "secret-token");
    private static final AccConnectionDto CONNECTION = new AccConnectionDto(1L, "WALLET", "Wallet", true, true,
            Map.of(), null, null, null, 360, null, false);

    @Test
    void shouldListConnections() throws Exception {
        when(connectionService.getConnections()).thenReturn(List.of(CONNECTION));

        perform(get(CONNECTIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wallet"))
                .andExpect(jsonPath("$[0].credentialsSet").value(true));
    }

    @Test
    void shouldGetConnection() throws Exception {
        when(connectionService.getConnection(1L)).thenReturn(CONNECTION);

        perform(get(CONNECTIONS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncIntervalMinutes").value(360));
    }

    @Test
    void shouldCreateConnectionWithoutReturningCredentials() throws Exception {
        when(connectionService.createConnection(any())).thenReturn(CONNECTION);

        perform(post(CONNECTIONS), new AccConnectionCreateRequestDto("WALLET", "Wallet", CREDENTIALS, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.credentials").doesNotExist());
    }

    @Test
    void shouldRejectConnectionWithoutCredentials() throws Exception {
        perform(post(CONNECTIONS), new AccConnectionCreateRequestDto("WALLET", "Wallet", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: credentials: must not be null"));
        verifyNoInteractions(connectionService);
    }

    @Test
    void shouldReturnBadRequestForUnknownConnectorType() throws Exception {
        when(connectionService.createConnection(any())).thenThrow(new UnknownConnectorTypeException("NOPE"));

        perform(post(CONNECTIONS), new AccConnectionCreateRequestDto("NOPE", "Wallet", CREDENTIALS, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown connector type: NOPE"));
    }

    @Test
    void shouldReturnUnprocessableContentWhenSourceRejectsCredentials() throws Exception {
        when(connectionService.createConnection(any())).thenThrow(new ConnectorAuthException("Invalid token"));

        perform(post(CONNECTIONS), new AccConnectionCreateRequestDto("WALLET", "Wallet", CREDENTIALS, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("ConnectorAuthException"))
                .andExpect(jsonPath("$.message").value("Invalid token"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void shouldUpdateConnection() throws Exception {
        when(connectionService.updateConnection(any(), any())).thenReturn(CONNECTION);

        perform(put(CONNECTIONS + "/1"), new AccConnectionUpdateRequestDto("Wallet", Map.of(), 360))
                .andExpect(status().isOk());

        verify(connectionService).updateConnection(1L, new AccConnectionUpdateRequestDto("Wallet", Map.of(), 360));
    }

    @Test
    void shouldRejectNonPositiveSyncInterval() throws Exception {
        perform(put(CONNECTIONS + "/1"), new AccConnectionUpdateRequestDto("Wallet", Map.of(), 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteConnection() throws Exception {
        perform(delete(CONNECTIONS + "/1")).andExpect(status().isNoContent());

        verify(connectionService).deleteConnection(1L);
    }

    @Test
    void shouldSetCredentials() throws Exception {
        when(connectionService.setCredentials(1L, CREDENTIALS)).thenReturn(CONNECTION);

        perform(put(CONNECTIONS + "/1/credentials"), Map.of("credentials", CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentials").doesNotExist());
    }

    @Test
    void shouldEnableConnection() throws Exception {
        when(connectionService.setEnabled(1L, false)).thenReturn(CONNECTION);

        perform(put(CONNECTIONS + "/1/enabled"), new AccConnectionEnabledRequestDto(false))
                .andExpect(status().isOk());

        verify(connectionService).setEnabled(1L, false);
    }

    @Test
    void shouldRejectEnabledRequestWithoutFlag() throws Exception {
        perform(put(CONNECTIONS + "/1/enabled"), new AccConnectionEnabledRequestDto(null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSetFallbackAccounts() throws Exception {
        var request = new AccConnectionFallbackRequestDto(5L, null);
        when(connectionService.setFallbackAccounts(1L, request)).thenReturn(CONNECTION);

        perform(put(CONNECTIONS + "/1/fallback-accounts"), request).andExpect(status().isOk());

        verify(connectionService).setFallbackAccounts(1L, request);
    }

    @Test
    void shouldReturnNoContentWhenConnectionTestPasses() throws Exception {
        perform(post(CONNECTIONS + "/1/test")).andExpect(status().isNoContent());

        verify(connectionService).testConnection(1L);
    }

    @Test
    void shouldReturnTooManyRequestsWithRetryAfterWhenSourceThrottles() throws Exception {
        doThrow(new ConnectorRateLimitedException("Slow down", Duration.ofSeconds(90)))
                .when(connectionService).testConnection(1L);

        perform(post(CONNECTIONS + "/1/test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "90"));
    }

    @Test
    void shouldReturnBadGatewayWhenSourceIsUnavailable() throws Exception {
        doThrow(new ConnectorTransientException("Source unavailable", new IOException("reset")))
                .when(connectionService).testConnection(1L);

        perform(post(CONNECTIONS + "/1/test"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Source unavailable"));
    }

    @Test
    void shouldStartSynchronizationInBackground() throws Exception {
        perform(post(CONNECTIONS + "/1/sync")).andExpect(status().isAccepted());

        verify(manualSyncService).syncNow(1L);
    }

    @Test
    void shouldListSynchronizationRuns() throws Exception {
        var run = new AccSyncRunDto(7L, SyncTrigger.MANUAL, SyncRunStatus.PARTIAL,
                Instant.parse("2026-02-01T08:00:00Z"), Instant.parse("2026-02-01T08:01:00Z"),
                10, 4, 1, 0, 3, 2, null);
        when(syncService.getRuns(1L)).thenReturn(List.of(run));

        perform(get(CONNECTIONS + "/1/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PARTIAL"))
                .andExpect(jsonPath("$[0].errors").value(2));
    }
}
