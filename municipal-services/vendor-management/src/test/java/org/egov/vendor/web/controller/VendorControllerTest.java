package org.egov.vendor.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.vendor.service.VendorService;
import org.egov.vendor.utils.ResponseInfoFactory;
import org.egov.vendor.web.models.VendorAdditionalDetails;
import org.egov.vendor.web.models.VendorAdditionalDetailsRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VendorControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VendorService vendorService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VendorController(vendorService, responseInfoFactory)).build();
    }

    @Test
    @DisplayName("createContractor_whenValidRequest_returns200WithResponseInfo")
    void createContractor_whenValidRequest_returns200WithResponseInfo() throws Exception {
        VendorAdditionalDetails details = new VendorAdditionalDetails();
        details.setTenantId("pg.citya");
        when(vendorService.saveVendor(any())).thenReturn(details);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), eq(true)))
                .thenReturn(ResponseInfo.builder().status("successful").build());

        VendorAdditionalDetailsRequest body = new VendorAdditionalDetailsRequest();
        body.setRequestInfo(RequestInfo.builder().build());
        body.setVendorAdditionalDetails(details);

        mockMvc.perform(post("/api/v1/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResponseInfo.status").value("successful"));
    }
}
