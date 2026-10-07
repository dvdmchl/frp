package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCreateMissingAccountsRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.service.ExternalMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTIONS + ApiPath.ID_PARAM + ApiPath.MAPPINGS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class ExternalMappingController {

    private final ExternalMappingService mappingService;

    @Operation(summary = "Get mappings",
            description = "Returns the external accounts and categories of a connection with their mapping.")
    @GetMapping
    public ResponseEntity<List<AccExternalMappingDto>> getMappings(@PathVariable Long id) {
        return ResponseEntity.ok(mappingService.getMappings(id));
    }

    @Operation(summary = "Update mapping",
            description = "Maps an external account or category to an account, or unmaps or ignores it.")
    @PutMapping(ApiPath.MAPPING_ID_PARAM)
    public ResponseEntity<AccExternalMappingDto> updateMapping(
            @PathVariable Long id, @PathVariable Long mappingId,
            @RequestBody AccExternalMappingUpdateRequestDto request) {
        return ResponseEntity.ok(mappingService.updateMapping(id, mappingId, request));
    }

    @Operation(summary = "Refresh mappings",
            description = "Fetches the external accounts and categories from the source; existing mappings stay.")
    @PostMapping(ApiPath.REFRESH)
    public ResponseEntity<List<AccExternalMappingDto>> refreshMappings(@PathVariable Long id) {
        return ResponseEntity.ok(mappingService.refreshMappings(id));
    }

    @Operation(summary = "Create missing accounts",
            description = "Creates and maps an account for every unmapped, not ignored external account or category.")
    @PostMapping(ApiPath.CREATE_MISSING_ACCOUNTS)
    public ResponseEntity<List<AccExternalMappingDto>> createMissingAccounts(
            @PathVariable Long id, @Valid @RequestBody AccCreateMissingAccountsRequestDto request) {
        return ResponseEntity.ok(mappingService.createMissingAccounts(id, request.kind(), request.parentNodeId()));
    }
}
