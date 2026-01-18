package org.dreamabout.sw.frp.be.module.accounting.controller;

import tools.jackson.databind.ObjectMapper;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.dreamabout.sw.frp.be.module.accounting.service.AccountService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;



class AccountControllerTest extends AbstractDbTest {





    @Autowired

    private MockMvc mockMvc;



    @Autowired

    private ObjectMapper objectMapper;



    @MockitoBean

    private AccountService accountService;



    @Test

    void createAccount_shouldReturn200() throws Exception {

        var request = new AccAccountCreateRequestDto(

                null, "Test", "Desc", "CZK", true, AccAcountType.ASSET, false

        );

        var response = new AccNodeDto(1L, null, false, null, 0, null);



        when(accountService.createAccount(any())).thenReturn(response);



        mockMvc.perform(post(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.ACCOUNTS)

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(request))

                        .with(user("testuser"))

                        .with(csrf()))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id").value(1));

    }

}
