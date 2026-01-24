package org.dreamabout.sw.frp.be.module.common.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        var adminEmail = "admin_maint@test.com";
        var adminPwd = "adminpassword";
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(adminEmail, adminPwd, "Admin User", null))))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE frp_public.frp_user SET admin = true WHERE email = ?", adminEmail);

        var loginRespJson = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequestDto(adminEmail, adminPwd))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readValue(loginRespJson, UserLoginResponseDto.class).token();
    }

    @Test
    void listOrphanSchemas_ok_test() throws Exception {
        jdbcTemplate.execute("CREATE SCHEMA orphan_maint_1");

        var json = mockMvc.perform(get(ApiPath.MAINTENANCE_FULL + ApiPath.ORPHAN_SCHEMAS)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> orphans = objectMapper.readValue(json, List.class);
        assertThat(orphans).contains("orphan_maint_1");
    }

    @Test
    void dropOrphanSchemas_ok_test() throws Exception {
        jdbcTemplate.execute("CREATE SCHEMA orphan_maint_2");

        mockMvc.perform(delete(ApiPath.MAINTENANCE_FULL + ApiPath.ORPHAN_SCHEMAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("orphan_maint_2")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        var json = mockMvc.perform(get(ApiPath.MAINTENANCE_FULL + ApiPath.ORPHAN_SCHEMAS)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> orphans = objectMapper.readValue(json, List.class);
        assertThat(orphans).doesNotContain("orphan_maint_2");
    }

    @Test
    void unauthorized_access_test() throws Exception {
        mockMvc.perform(get(ApiPath.MAINTENANCE_FULL + ApiPath.ORPHAN_SCHEMAS))
                .andExpect(status().isForbidden());
    }
}
