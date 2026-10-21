package org.egov.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.service.ApportionService;
import org.egov.util.ResponseInfoFactory;
import org.egov.web.models.ApportionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApportionControllerWebTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApportionService apportionService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    private ApportionController controller;

    @BeforeEach
    void setUp() {
        controller = new ApportionController(objectMapper, apportionService);
        ReflectionTestUtils.setField(controller, "responseInfoFactory", responseInfoFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void apportion_returnsOk() throws Exception {
        when(apportionService.apportionBills(any())).thenReturn(Collections.emptyList());
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(ResponseInfo.builder().status("successful").build());

        ApportionRequest request = new ApportionRequest();
        request.setRequestInfo(RequestInfo.builder().apiId("apportion").build());
        request.setTenantId("pb.amritsar");

        mockMvc.perform(post("/v1/_apportion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
