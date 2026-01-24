package org.dreamabout.sw.frp.be.module.common.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateGroupsRequestDto;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private Long targetUserId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Create Admin
        var adminEmail = "admin@test.com";
        var adminPwd = "adminpassword";
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(adminEmail, adminPwd, "Admin User", null))))
                .andExpect(status().isOk());

        // Make it admin manually in DB
        jdbcTemplate.update("UPDATE frp_public.frp_user SET admin = true WHERE email = ?", adminEmail);

        var loginRespJson = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequestDto(adminEmail, adminPwd))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readValue(loginRespJson, UserLoginResponseDto.class).token();

        // 2. Create Target User
        var userEmail = "user@test.com";
        var userJson = mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(userEmail, "userpwd", "Standard User", null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        targetUserId = objectMapper.readValue(userJson, UserDto.class).id();
    }

    @Test
    void listUsers_ok_test() throws Exception {
        var json = mockMvc.perform(get(ApiPath.ADMIN_USERS_FULL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UserDto[] users = objectMapper.readValue(json, UserDto[].class);
        assertThat(users).hasSize(2);
    }

    @Test
    void searchUsers_ok_test() throws Exception {
        var json = mockMvc.perform(get(ApiPath.ADMIN_USERS_FULL)
                        .param("query", "Standard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UserDto[] users = objectMapper.readValue(json, UserDto[].class);
        assertThat(users).hasSize(1);
        assertThat(users[0].fullName()).isEqualTo("Standard User");
    }

    @Test
    void getUser_ok_test() throws Exception {
        var json = mockMvc.perform(get(ApiPath.ADMIN_USERS_FULL + "/" + targetUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var user = objectMapper.readValue(json, UserDto.class);
        assertThat(user.id()).isEqualTo(targetUserId);
    }

    @Test
    void getUser_notFound_test() throws Exception {
        mockMvc.perform(get(ApiPath.ADMIN_USERS_FULL + "/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateActiveStatus_ok_test() throws Exception {
        var json = mockMvc.perform(patch(ApiPath.ADMIN_USERS_FULL + ApiPath.ID_ACTIVE, targetUserId)
                        .param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var user = objectMapper.readValue(json, UserDto.class);
        assertThat(user.active()).isFalse();
    }

    @Test
    void updateAdminStatus_ok_test() throws Exception {
        var json = mockMvc.perform(patch(ApiPath.ADMIN_USERS_FULL + ApiPath.ID_ADMIN, targetUserId)
                        .param("admin", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var user = objectMapper.readValue(json, UserDto.class);
        assertThat(user.admin()).isTrue();
    }

    @Test
    void updateGroups_ok_test() throws Exception {
        var request = new UserUpdateGroupsRequestDto(Set.of());
        var json = mockMvc.perform(put(ApiPath.ADMIN_USERS_FULL + ApiPath.ID_GROUPS, targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var user = objectMapper.readValue(json, UserDto.class);
        assertThat(user.id()).isEqualTo(targetUserId);
    }

    @Test
    void unauthorized_access_test() throws Exception {
        mockMvc.perform(get(ApiPath.ADMIN_USERS_FULL))
                .andExpect(status().isForbidden());
    }
}
