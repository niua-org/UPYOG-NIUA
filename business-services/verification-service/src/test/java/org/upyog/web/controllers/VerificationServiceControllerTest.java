package org.upyog.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.service.CommonService;
import org.upyog.web.models.CommonDetails;
import org.upyog.web.models.ModuleSearchCriteria;
import org.upyog.web.models.ModuleSearchRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VerificationServiceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CommonService commonService;

    @InjectMocks
    private VerificationServiceController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void search_returnsApplicationDetails() throws Exception {
        when(commonService.getApplicationCommonDetails(any())).thenReturn(CommonDetails.builder().build());

        ModuleSearchRequest request = ModuleSearchRequest.builder()
                .moduleSearchCriteria(ModuleSearchCriteria.builder()
                        .applicationNumber("APP-001")
                        .moduleName("TL")
                        .tenantId("pb.amritsar")
                        .build())
                .build();

        mockMvc.perform(post("/validity/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CommonDetail").exists());
    }
}
