package org.dreamabout.sw.frp.be.module.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.ModuleDefinitionDto;
import org.dreamabout.sw.frp.be.module.common.service.ModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.MODULES_FULL)
@RequiredArgsConstructor
@Tag(name = "Module Management", description = "Read-only access to available modules and their state")
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping
    @Operation(summary = "List all modules", description = "Returns a list of all available modules and their state.")
    public ResponseEntity<List<ModuleDefinitionDto>> listModules() {
        return ResponseEntity.ok(moduleService.getAllModules());
    }

    @GetMapping(ApiPath.CODE_PARAM)
    @Operation(summary = "Get module details", description = "Returns details of a specific module by code.")
    public ResponseEntity<ModuleDefinitionDto> getModule(@PathVariable String code) {
        return moduleService.getModule(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
