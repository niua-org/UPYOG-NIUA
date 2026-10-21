package org.egov.vendor.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.vendor.service.VendorService;
import org.egov.vendor.utils.ResponseInfoFactory;
import org.egov.vendor.web.models.VendorAdditionalDetails;
import org.egov.vendor.web.models.VendorAdditionalDetailsRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorController.class)
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VendorService vendorService;

    @MockBean
    private ResponseInfoFactory responseInfoFactory;

    @Test
    @DisplayName("createContractor_whenValidRequest_returns200WithResponseInfo")
    void createContractor_whenValidRequest_returns200WithResponseInfo() throws Exception {
        VendorAdditionalDetails details = VendorAdditionalDetails.builder().tenantId("pg.citya").build();
        when(vendorService.saveVendor(any())).thenReturn(details);
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(org.egov.common.contract.response.ResponseInfo.builder().status("successful").build());

        VendorAdditionalDetailsRequest body = VendorAdditionalDetailsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .vendorAdditionalDetails(details)
                .build();

        mockMvc.perform(post("/api/v1/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResponseInfo.status").value("successful"));
    }

}
