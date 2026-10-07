package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectorDto;
import org.dreamabout.sw.frp.be.module.accounting.service.ConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTORS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class ConnectorController {

    private final ConnectionService connectionService;

    @Operation(summary = "Get connectors",
            description = "Returns the connector types available for new connections with their credential fields.")
    @GetMapping
    public ResponseEntity<List<AccConnectorDto>> getConnectors() {
        return ResponseEntity.ok(connectionService.getConnectors());
    }
}
