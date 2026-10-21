package org.egov.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.service.ApportionServiceV2;
import org.egov.util.ResponseInfoFactory;
import org.egov.web.models.ApportionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApportionControllerV2WebTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApportionServiceV2 apportionService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @BeforeEach
    void setUp() {
        ApportionControllerV2 controller = new ApportionControllerV2(objectMapper, apportionService, responseInfoFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void apportionBills_returnsOk() throws Exception {
        when(apportionService.apportionBills(any())).thenReturn(Collections.emptyList());
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(ResponseInfo.builder().status("successful").build());

        ApportionRequest request = ApportionRequest.builder()
                .requestInfo(new RequestInfo())
                .tenantId("pb")
                .bills(Collections.emptyList())
                .build();

        mockMvc.perform(post("/v2/bill/_apportion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
