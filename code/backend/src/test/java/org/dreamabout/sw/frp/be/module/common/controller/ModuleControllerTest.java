package org.dreamabout.sw.frp.be.module.common.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.domain.ModuleState;
import org.dreamabout.sw.frp.be.module.common.model.dto.ModuleDefinitionDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.service.ModuleService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModuleControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModuleService moduleService;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        var email = "schema@tester.com";
        var password = "password";
        var fullName = "Schema Tester";

        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(email, password, fullName, null))))
                .andExpect(status().isOk());

        var loginResp = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequestDto(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var loginDto = objectMapper.readValue(loginResp, UserLoginResponseDto.class);
        this.token = loginDto.token();
    }

    @Test
    void listModules_shouldReturnList() throws Exception {
        var modules = List.of(
                new ModuleDefinitionDto("ACC", "Accounting", "Desc", ModuleState.ENABLED),
                new ModuleDefinitionDto("TSK", "Tasks", "Desc", ModuleState.DISABLED)
        );
        given(moduleService.getAllModules()).willReturn(modules);

        mockMvc.perform(get(ApiPath.MODULES_FULL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACC"))
                .andExpect(jsonPath("$[1].code").value("TSK"));
    }

    @Test
    void getModule_shouldReturnModule() throws Exception {
        var module = new ModuleDefinitionDto("ACC", "Accounting", "Desc", ModuleState.ENABLED);
        given(moduleService.getModule("ACC")).willReturn(Optional.of(module));

        mockMvc.perform(get(ApiPath.MODULES_FULL + "/ACC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACC"))
                .andExpect(jsonPath("$.state").value("ENABLED"));
    }

    @Test
    void getModule_shouldReturnNotFound() throws Exception {
        given(moduleService.getModule("UNKNOWN")).willReturn(Optional.empty());

        mockMvc.perform(get(ApiPath.MODULES_FULL + "/UNKNOWN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
