package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.frp.be.module.common.domain.ModuleState;
import org.dreamabout.sw.frp.be.module.common.model.dto.ModuleDefinitionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleServiceTest {

    private ModuleService moduleService;

    @BeforeEach
    void setUp() {
        moduleService = new ModuleService();
        moduleService.init();
    }

    @Test
    void getAllModules_shouldReturnLoadedModules() {
        var modules = moduleService.getAllModules();
        assertThat(modules).isNotEmpty();
        assertThat(modules).extracting(ModuleDefinitionDto::code).contains("ACC", "TSK");
    }

    @Test
    void getModule_shouldReturnModuleByCode() {
        var module = moduleService.getModule("ACC");
        assertThat(module).isPresent();
        assertThat(module.get().code()).isEqualTo("ACC");
        assertThat(module.get().state()).isEqualTo(ModuleState.ENABLED);
    }

    @Test
    void getModule_shouldReturnEmptyForUnknownCode() {
        var module = moduleService.getModule("UNKNOWN");
        assertThat(module).isEmpty();
    }
}
