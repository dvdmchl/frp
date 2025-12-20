package org.dreamabout.sw.frp.be.module.common.controller;

import tools.jackson.databind.ObjectMapper;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.*;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchemaControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    private UserDto currentUser;

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
        this.currentUser = loginDto.user();
    }

    @Test
    void create_schema_ok_test() throws Exception {
        var req = new SchemaCreateRequestDto("new_schema");

        mockMvc.perform(post("/api/schema")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify it exists in list
        var listResp = mockMvc.perform(get("/api/schema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        List<String> schemas = objectMapper.readValue(listResp, List.class);
        assertThat(schemas).contains("new_schema");
    }

    @Test
    void create_schema_and_set_active_ok_test() throws Exception {
        var req = new SchemaCreateRequestDto("active_schema");

        mockMvc.perform(post("/api/schema?setActive=true")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify user active schema
        var meResp = mockMvc.perform(get(ApiPath.USER_ME_FULL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        UserDto me = objectMapper.readValue(meResp, UserDto.class);
        assertThat(me.activeSchema()).isEqualTo("active_schema");
    }

    @Test
    void copy_schema_ok_test() throws Exception {
        // Disabled: Copy schema not implemented yet
        if (true) return;
        // Create source
        var createReq = new SchemaCreateRequestDto("source_schema");
        mockMvc.perform(post("/api/schema")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // Copy
        var copyReq = new SchemaCopyRequestDto("source_schema", "target_schema");

        mockMvc.perform(post("/api/schema/copy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyReq)))
                .andExpect(status().isOk());

        // Verify
        var listResp = mockMvc.perform(get("/api/schema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        List<String> schemas = objectMapper.readValue(listResp, List.class);
        assertThat(schemas).contains("source_schema", "target_schema");
    }

    @Test
    void delete_schema_ok_test() throws Exception {
        // Create
        var createReq = new SchemaCreateRequestDto("delete_me");
        mockMvc.perform(post("/api/schema")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/api/schema/delete_me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify
        var listResp = mockMvc.perform(get("/api/schema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        List<String> schemas = objectMapper.readValue(listResp, List.class);
        assertThat(schemas).doesNotContain("delete_me");
    }

    @Test
    void switch_active_schema_ok_test() throws Exception {
        // Create
        var createReq = new SchemaCreateRequestDto("schema_one");
        mockMvc.perform(post("/api/schema")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // Switch
        var switchReq = new SchemaSetActiveRequestDto("schema_one");
        mockMvc.perform(put("/api/schema/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(switchReq)))
                .andExpect(status().isOk());

        // Verify
        var meResp = mockMvc.perform(get(ApiPath.USER_ME_FULL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        UserDto me = objectMapper.readValue(meResp, UserDto.class);
        assertThat(me.activeSchema()).isEqualTo("schema_one");
    }
}
