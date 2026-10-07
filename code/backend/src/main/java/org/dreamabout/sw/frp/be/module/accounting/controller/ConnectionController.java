package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCredentialsRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionEnabledRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionFallbackRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccSyncRunDto;
import org.dreamabout.sw.frp.be.module.accounting.service.ConnectionService;
import org.dreamabout.sw.frp.be.module.accounting.service.ManualSyncService;
import org.dreamabout.sw.frp.be.module.accounting.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTIONS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final ManualSyncService manualSyncService;
    private final SyncService syncService;

    @Operation(summary = "Get connections", description = "Returns all connections to external sources.")
    @GetMapping
    public ResponseEntity<List<AccConnectionDto>> getConnections() {
        return ResponseEntity.ok(connectionService.getConnections());
    }

    @Operation(summary = "Get connection", description = "Returns a connection by ID; credentials are never returned.")
    @GetMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccConnectionDto> getConnection(@PathVariable Long id) {
        return ResponseEntity.ok(connectionService.getConnection(id));
    }

    @Operation(summary = "Create connection",
            description = "Creates a connection after the source accepted its credentials.")
    @PostMapping
    public ResponseEntity<AccConnectionDto> createConnection(
            @Valid @RequestBody AccConnectionCreateRequestDto request) {
        return ResponseEntity.ok(connectionService.createConnection(request));
    }

    @Operation(summary = "Update connection", description = "Changes the name and synchronization settings.")
    @PutMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccConnectionDto> updateConnection(@PathVariable Long id,
                                                             @Valid @RequestBody AccConnectionUpdateRequestDto request) {
        return ResponseEntity.ok(connectionService.updateConnection(id, request));
    }

    @Operation(summary = "Delete connection", description = "Deletes a connection; its posted transactions stay.")
    @DeleteMapping(ApiPath.ID_PARAM)
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set connection credentials",
            description = "Sets or rotates the write-only credentials after the source accepted them.")
    @PutMapping(ApiPath.ID_PARAM + ApiPath.CREDENTIALS)
    public ResponseEntity<AccConnectionDto> setCredentials(
            @PathVariable Long id, @Valid @RequestBody AccConnectionCredentialsRequestDto request) {
        return ResponseEntity.ok(connectionService.setCredentials(id, request.credentials()));
    }

    @Operation(summary = "Enable or disable connection", description = "A disabled connection is not synchronized.")
    @PutMapping(ApiPath.ID_PARAM + ApiPath.ENABLED)
    public ResponseEntity<AccConnectionDto> setEnabled(@PathVariable Long id,
                                                       @Valid @RequestBody AccConnectionEnabledRequestDto request) {
        return ResponseEntity.ok(connectionService.setEnabled(id, request.enabled()));
    }

    @Operation(summary = "Set fallback accounts",
            description = "Sets the accounts for records whose category is not mapped.")
    @PutMapping(ApiPath.ID_PARAM + ApiPath.FALLBACK_ACCOUNTS)
    public ResponseEntity<AccConnectionDto> setFallbackAccounts(@PathVariable Long id,
                                                                @RequestBody AccConnectionFallbackRequestDto request) {
        return ResponseEntity.ok(connectionService.setFallbackAccounts(id, request));
    }

    @Operation(summary = "Test connection", description = "Verifies the stored credentials against the source.")
    @PostMapping(ApiPath.ID_PARAM + ApiPath.TEST)
    public ResponseEntity<Void> testConnection(@PathVariable Long id) {
        connectionService.testConnection(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Synchronize connection",
            description = "Starts a synchronization in the background; its outcome appears in the run history.")
    @PostMapping(ApiPath.ID_PARAM + ApiPath.SYNC)
    public ResponseEntity<Void> syncNow(@PathVariable Long id) {
        manualSyncService.syncNow(id);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Get synchronization runs",
            description = "Returns the synchronization history of a connection, the latest first.")
    @GetMapping(ApiPath.ID_PARAM + ApiPath.RUNS)
    public ResponseEntity<List<AccSyncRunDto>> getRuns(@PathVariable Long id) {
        return ResponseEntity.ok(syncService.getRuns(id));
    }
}
