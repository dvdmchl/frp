package org.dreamabout.sw.frp.be.module.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.MAINTENANCE_FULL)
@Tag(name = "Maintenance (Admin)", description = "Database and system maintenance operations")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceController {

    private final SchemaService schemaService;

    @GetMapping(ApiPath.ORPHAN_SCHEMAS)
    @Operation(summary = "List orphan schemas", description = "Returns schemas that are not tracked in the frp_schema table.")
    public ResponseEntity<List<String>> listOrphanSchemas() {
        return ResponseEntity.ok(schemaService.getOrphanSchemas());
    }

    @DeleteMapping(ApiPath.ORPHAN_SCHEMAS)
    @Operation(summary = "Drop orphan schemas", description = "Drops the specified orphan schemas.")
    public ResponseEntity<Void> dropOrphanSchemas(@RequestBody List<String> schemaNames) {
        schemaService.dropOrphanSchemas(schemaNames);
        return ResponseEntity.noContent().build();
    }
}
