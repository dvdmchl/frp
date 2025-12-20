package org.dreamabout.sw.frp.be.module.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.common.model.dto.SchemaCopyRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.SchemaCreateRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.SchemaSetActiveRequestDto;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.module.common.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schema")
@Tag(name = "Schema Management", description = "Create, copy, delete, and switch active schema")
public class SchemaController {

    private final SchemaService schemaService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "List my schemas")
    public ResponseEntity<List<String>> listMySchemas() {
        var user = userService.getPrincipal();
        var schemas = schemaService.listMySchemas(user.getId())
                .stream().map(s -> s.getName()).toList();
        return ResponseEntity.ok(schemas);

    }

    @PostMapping
    @Operation(summary = "Create new schema")
    public ResponseEntity<Map<String, String>> createSchema(@RequestBody SchemaCreateRequestDto req,
                                                            @RequestParam(name = "setActive", defaultValue = "false") boolean setActive) {
        var user = userService.getPrincipal();
        var schema = schemaService.createSchema(req.name(), user.getId());
        if (setActive) {
            schemaService.setActiveSchema(schema.getName(), user.getId());
        }
        return ResponseEntity.ok(Map.of("name", schema.getName()));
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy schema")
    public ResponseEntity<Map<String, String>> copySchema(@RequestBody SchemaCopyRequestDto req) {
        var user = userService.getPrincipal();
        var schema = schemaService.copySchema(req.source(), req.target(), user.getId());
        return ResponseEntity.ok(Map.of("name", schema.getName()));
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete schema")
    public ResponseEntity<Void> deleteSchema(@PathVariable("name") String name) {
        var user = userService.getPrincipal();
        schemaService.deleteSchema(name, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/active")
    @Operation(summary = "Set active schema")
    public ResponseEntity<Void> setActiveSchema(@RequestBody SchemaSetActiveRequestDto req) {
        var user = userService.getPrincipal();
        schemaService.setActiveSchema(req.name(), user.getId());
        return ResponseEntity.ok().build();
    }
}
