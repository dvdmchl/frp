package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.service.JournalService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JournalControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JournalService journalService;

    @Test
    @WithMockUser
    void getAllJournals_shouldReturnList() throws Exception {
        var journalDto = new AccJournalDto(1L, LocalDate.now(), "Desc", 100L, BigDecimal.TEN, BigDecimal.ZERO);
        when(journalService.getAllJournals()).thenReturn(List.of(journalDto));

        mockMvc.perform(get(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.JOURNALS)
                        .with(user("testuser"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
