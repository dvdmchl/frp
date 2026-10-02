package org.dreamabout.sw.frp.be.module.common.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.AuditLogDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.AuditLogPageDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.SchemaCopyRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogControllerTest extends AbstractDbTest {

    private static final String ADMIN_EMAIL = "audit_admin@test.com";
    private static final String ADMIN_PWD = "adminpassword";
    private static final String ADMIN_SCHEMA = "audit_admin_schema";
    private static final String USER_EMAIL = "audit_user@test.com";
    private static final String USER_PWD = "userpassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        adminId = register(ADMIN_EMAIL, ADMIN_PWD, "Audit Admin");
        jdbcTemplate.update("UPDATE frp_public.frp_user SET admin = true WHERE email = ?", ADMIN_EMAIL);
        adminToken = login(ADMIN_EMAIL, ADMIN_PWD);
        userId = register(USER_EMAIL, USER_PWD, "Audit User");
    }

    @Test
    void registrationLoginAndSchemaCreation_areAudited_test() throws Exception {
        var login = single(search("?action=LOGIN"));
        assertThat(login.userId()).isEqualTo(adminId);
        assertThat(login.userEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(login.resource()).isEqualTo("user:" + adminId);
        assertThat(login.createdAt()).isNotNull();

        var registrations = search("?action=USER_REGISTERED").items();
        assertThat(registrations).extracting(AuditLogDto::userEmail).containsExactly(USER_EMAIL, ADMIN_EMAIL);
        assertThat(registrations).extracting(AuditLogDto::details).contains("schema=" + ADMIN_SCHEMA);

        var schemas = search("?action=SCHEMA_CREATED").items();
        assertThat(schemas).extracting(AuditLogDto::resource)
                .containsExactlyInAnyOrder("schema:" + ADMIN_SCHEMA, "schema:audit_user_schema");
    }

    @Test
    void failedLogin_isAudited_test() throws Exception {
        mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequestDto(USER_EMAIL, "wrong"))))
                .andExpect(status().isUnauthorized());

        var failed = single(search("?action=LOGIN_FAILED"));
        assertThat(failed.userId()).isEqualTo(userId);
        assertThat(failed.userEmail()).isEqualTo(USER_EMAIL);
        assertThat(failed.details()).isEqualTo("BadCredentialsException");
    }

    @Test
    void adminChanges_areAuditedWithActingAdmin_test() throws Exception {
        mockMvc.perform(patch(ApiPath.ADMIN_USERS_FULL + "/" + userId + ApiPath.ADMIN)
                        .param("admin", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch(ApiPath.ADMIN_USERS_FULL + "/" + userId + ApiPath.ACTIVE)
                        .param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var granted = single(search("?action=ADMIN_GRANTED"));
        assertThat(granted.userId()).isEqualTo(adminId);
        assertThat(granted.userEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(granted.resource()).isEqualTo("user:" + userId);

        var deactivated = single(search("?action=USER_DEACTIVATED"));
        assertThat(deactivated.userId()).isEqualTo(adminId);
        assertThat(deactivated.resource()).isEqualTo("user:" + userId);
    }

    @Test
    void schemaCopyAndDrop_areAuditedFromLibraryEvents_test() throws Exception {
        mockMvc.perform(post(ApiPath.SCHEMA_FULL + ApiPath.COPY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SchemaCopyRequestDto(ADMIN_SCHEMA, "audit_copy")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete(ApiPath.SCHEMA_FULL + "/audit_copy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        var copied = single(search("?action=SCHEMA_COPIED"));
        assertThat(copied.userId()).isEqualTo(adminId);
        assertThat(copied.resource()).isEqualTo("schema:audit_copy");
        assertThat(copied.details()).isEqualTo("source=" + ADMIN_SCHEMA);

        var dropped = single(search("?action=SCHEMA_DROPPED"));
        assertThat(dropped.userId()).isEqualTo(adminId);
        assertThat(dropped.resource()).isEqualTo("schema:audit_copy");
    }

    @Test
    void search_filtersByUserAndPages_test() throws Exception {
        var adminEntries = search("?userId=" + adminId);
        assertThat(adminEntries.items()).isNotEmpty().allMatch(e -> adminId.equals(e.userId()));

        var page = search("?size=1&page=1");
        assertThat(page.items()).hasSize(1);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isGreaterThan(1);
        assertThat(page.totalPages()).isEqualTo((int) page.totalElements());

        var future = search("?from=2999-01-01T00:00:00Z");
        assertThat(future.items()).isEmpty();
        assertThat(future.totalElements()).isZero();

        var past = search("?to=2000-01-01T00:00:00Z");
        assertThat(past.items()).isEmpty();
    }

    @Test
    void nonAdmin_isForbidden_test() throws Exception {
        var userToken = login(USER_EMAIL, USER_PWD);
        mockMvc.perform(get(ApiPath.AUDIT_LOG_FULL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPath.AUDIT_LOG_FULL))
                .andExpect(status().isForbidden());
    }

    private Long register(String email, String password, String fullName) throws Exception {
        var json = mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(email, password, fullName, null))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, UserDto.class).id();
    }

    private String login(String email, String password) throws Exception {
        var json = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserLoginRequestDto(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, UserLoginResponseDto.class).token();
    }

    private AuditLogPageDto search(String query) throws Exception {
        var json = mockMvc.perform(get(ApiPath.AUDIT_LOG_FULL + query)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AuditLogPageDto.class);
    }

    private static AuditLogDto single(AuditLogPageDto page) {
        assertThat(page.items()).hasSize(1);
        return page.items().getFirst();
    }
}
