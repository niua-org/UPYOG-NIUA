package org.egov.pgr.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.pgr.service.PGRService;
import org.egov.pgr.util.ResponseInfoFactory;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RequestsApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PGRService pgrService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @BeforeEach
    void setUp() {
        RequestsApiController controller = new RequestsApiController(new ObjectMapper(), pgrService, responseInfoFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("requestsCreatePost_whenServiceReturnsRequest_returns200")
    void requestsCreatePost_whenServiceReturnsRequest_returns200() throws Exception {
        ServiceRequest persisted = ServiceRequest.builder()
                .service(Service.builder().tenantId("pg.citya").build())
                .build();
        when(pgrService.create(any())).thenReturn(persisted);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), eq(true)))
                .thenReturn(ResponseInfo.builder().status("successful").build());

        mockMvc.perform(post("/v2/request/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"RequestInfo\":{},\"service\":{\"tenantId\":\"pg.citya\"}}"))
                .andExpect(status().isOk());
    }
}
