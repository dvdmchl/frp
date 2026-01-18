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
        // Create source
        var createReq = new SchemaCreateRequestDto("source_schema");
        mockMvc.perform(post("/api/schema")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());

        // Insert some data into source
        jdbcTemplate.execute("""
            INSERT INTO source_schema.acc_currency (id, code, name, scale, created_by_user_id, created_at, updated_by_user_id, updated_at, version, is_base) 
            VALUES (100, 'USD', 'US Dollar', 2, 1, NOW(), 1, NOW(), 0, false);
            
            INSERT INTO source_schema.acc_account (id, name, currency_id, is_liquid, account_type, created_by_user_id, created_at, updated_by_user_id, updated_at, version)
            VALUES (1, 'Cash', 100, true, 'ASSET', 1, NOW(), 1, NOW(), 0);
            
            INSERT INTO source_schema.acc_transaction (id, created_by_user_id, created_at, updated_by_user_id, updated_at, version)
            VALUES (1, 1, NOW(), 1, NOW(), 0);

            INSERT INTO source_schema.acc_journal (id, account_id, transaction_id, date, credit, debit, created_by_user_id, created_at, updated_by_user_id, updated_at, version) 
            VALUES (1, 1, 1, '2024-01-01', 100, 0, 1, NOW(), 1, NOW(), 0);
        """);

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

        // Verify data was copied
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM target_schema.acc_journal", Integer.class);
        assertThat(count).isEqualTo(1);

        // Verify sequence was updated by inserting another record and checking ID
        jdbcTemplate.execute("""
            INSERT INTO target_schema.acc_journal (account_id, transaction_id, date, credit, debit, created_by_user_id, created_at, updated_by_user_id, updated_at, version) 
            VALUES (1, 1, '2024-01-02', 50, 0, 1, NOW(), 1, NOW(), 0);
        """);
        Integer nextId = jdbcTemplate.queryForObject("SELECT max(id) FROM target_schema.acc_journal", Integer.class);
        assertThat(nextId).isEqualTo(2);
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
        assertThat(schemas).isNotEmpty().doesNotContain("delete_me");
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
