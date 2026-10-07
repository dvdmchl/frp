package org.dreamabout.sw.frp.be.module.accounting.controller;

import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.service.ConnectionService;
import org.dreamabout.sw.frp.be.module.accounting.service.ExternalMappingService;
import org.dreamabout.sw.frp.be.module.accounting.service.ImportRecordService;
import org.dreamabout.sw.frp.be.module.accounting.service.ManualSyncService;
import org.dreamabout.sw.frp.be.module.accounting.service.SyncService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Mocks all services behind the connection API, so its controller tests share one application context.
 */
abstract class AbstractConnectionApiTest extends AbstractDbTest {

    protected static final String CONNECTIONS = ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTIONS;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected ConnectionService connectionService;
    @MockitoBean
    protected ManualSyncService manualSyncService;
    @MockitoBean
    protected SyncService syncService;
    @MockitoBean
    protected ExternalMappingService mappingService;
    @MockitoBean
    protected ImportRecordService importRecordService;

    protected ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("testuser")).with(csrf()));
    }

    protected ResultActions perform(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return perform(request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }
}
