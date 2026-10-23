package org.upyog.family.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.FamilyHeadService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.advice.GlobalExceptionHandler;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FamilyHeadControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FamilyHeadService headService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FamilyHeadController headController;

    private Family family;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(headController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("member-uuid-2")
                .build();

        org.mockito.Mockito.lenient().when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(new ResponseInfo());
    }

    @Test
    @DisplayName("POST /v1/_assignHof should return HTTP 200 OK")
    void assignHeadOfFamily_Success() throws Exception {
        AssignHofRequest request = AssignHofRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("member-uuid-2")
                .reason("Seniority transfer")
                .build();

        when(headService.assignHeadOfFamily(any(AssignHofRequest.class))).thenReturn(family);

        mockMvc.perform(post("/v1/_assignHof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].headUserUuid").value("member-uuid-2"));
    }
}
