package org.dreamabout.sw.frp.be.module.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserLoginResponseDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserRegisterRequestDto;
import org.dreamabout.sw.frp.be.module.common.model.dto.UserUpdateRequestDto;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ok_test() throws Exception {
        var email = "john.doe_new@test.com";
        var fullName = "John Doe";
        var password = "password";
        var userRegisterDto = new UserRegisterRequestDto(email, password, fullName);

        var jsonReply = mockMvc.perform(
                        post(ApiPath.USER_REGISTER_FULL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRegisterDto))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var userDto = objectMapper.readValue(jsonReply, UserDto.class);
        assertThat(userDto.id()).isNotNull();
        assertThat(userDto.email()).isEqualTo(email);
        assertThat(userDto.fullName()).isEqualTo(fullName);
    }

    @Test
    void register_email_already_exists_test() throws Exception {
        var dto = new UserRegisterRequestDto("existing@test.com", "pwd", "Someone");

        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());


        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_empty_password_test() throws Exception {
        var dto = new UserRegisterRequestDto("empty@pwd.com", "", "NoPass");
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void login_ok_test() throws Exception {
        var email = "john.doe@test.com";
        var fullName = "John Doe";
        var password = "password";
        var registerDto = new UserRegisterRequestDto(email, password, fullName);
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk());

        var loginDto = new UserLoginRequestDto(email, password);
        var jsonReply = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var userDto = objectMapper.readValue(jsonReply, UserLoginResponseDto.class);
        assertThat(userDto.user().email()).isEqualTo(email);
    }

    @Test
    void login_wrong_password_test() throws Exception {
        var email = "real@user.com";
        var password = "correct";
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRegisterRequestDto(email, password, "Real User"))))
                .andExpect(status().isOk());

        var loginDto = new UserLoginRequestDto(email, "wrongpass");
        mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticated_user_ok_test() throws Exception {
        var email = "me@test.com";
        var fullName = "Me Tester";
        var password = "pass";

        var registerDto = new UserRegisterRequestDto(email, password, fullName);
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk());

        var loginDto = new UserLoginRequestDto(email, password);
        var json = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var loginResponse = objectMapper.readValue(json, UserLoginResponseDto.class);
        var token = loginResponse.token();

        var response = mockMvc.perform(get(ApiPath.USER_ME_FULL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var userDto = objectMapper.readValue(response, UserDto.class);
        assertThat(userDto.email()).isEqualTo(email);
        assertThat(userDto.fullName()).isEqualTo(fullName);
    }

    @Test
    void authenticated_user_unauthorized_test() throws Exception {
        mockMvc.perform(get(ApiPath.USER_ME_FULL))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_user_ok_test() throws Exception {
        var email = "upd@test.com";
        var fullName = "Update Me";
        var password = "pass";
        var registerDto = new UserRegisterRequestDto(email, password, fullName);
        mockMvc.perform(post(ApiPath.USER_REGISTER_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk());

        var loginDto = new UserLoginRequestDto(email, password);
        var loginJson = mockMvc.perform(post(ApiPath.USER_LOGIN_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var loginResp = objectMapper.readValue(loginJson, UserLoginResponseDto.class);
        var token = loginResp.token();

        var updateDto = new UserUpdateRequestDto("New Name", "new@email.com", "newpass");
        var json = mockMvc.perform(put(ApiPath.USER_UPDATE_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var userDto = objectMapper.readValue(json, UserDto.class);
        assertThat(userDto.fullName()).isEqualTo("New Name");
        assertThat(userDto.email()).isEqualTo("new@email.com");
    }

    @Test
    void update_user_unauthorized_test() throws Exception {
        var dto = new UserUpdateRequestDto("Name", "email@test.com", "pwd");
        mockMvc.perform(put(ApiPath.USER_UPDATE_FULL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }


}
