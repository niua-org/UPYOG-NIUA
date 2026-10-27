package org.upyog.family.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.FamilyService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.advice.GlobalExceptionHandler;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySplitRequest;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FamilyControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FamilyService familyService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FamilyController familyController;

    private Family family;
    private FamilyRequest familyRequest;
    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(familyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        requestInfo = RequestInfo.builder().apiId("org.upyog.family").build();

        FamilyMember head = FamilyMember.builder()
                .userUuid("user-uuid-head")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();

        family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(Collections.singletonList(head))
                .build();

        familyRequest = FamilyRequest.builder()
                .requestInfo(requestInfo)
                .family(family)
                .build();

        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), eq(true)))
                .thenReturn(ResponseInfo.builder().status("successful").build());
    }

    @Test
    @DisplayName("POST /v1/family/_create should return 201 Created")
    void createFamily_Endpoint_Success() throws Exception {
        when(familyService.createFamily(any())).thenReturn(family);

        mockMvc.perform(post("/v1/family/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(familyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"))
                .andExpect(jsonPath("$.ResponseInfo.status").value("successful"));
    }

    @Test
    @DisplayName("POST /v1/family/_search should return 200 OK")
    void searchFamilies_Endpoint_Success() throws Exception {
        when(familyService.searchFamilies(any(), any())).thenReturn(Collections.singletonList(family));

        mockMvc.perform(post("/v1/family/_search")
                        .param("tenantId", "pg.citya")
                        .param("familyId", "FAM-2026-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }

    @Test
    @DisplayName("POST /v1/family/_update should return 200 OK")
    void updateFamily_Endpoint_Success() throws Exception {
        when(familyService.updateFamily(any())).thenReturn(family);

        mockMvc.perform(post("/v1/family/_update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(familyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }

    @Test
    @DisplayName("POST /v1/family/_split should return 201 Created")
    void splitFamily_Endpoint_Success() throws Exception {
        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .requestInfo(requestInfo)
                .tenantId("pg.citya")
                .parentFamilyId("FAM-2026-0001")
                .newFamilyHeadUserUuid("user-uuid-son")
                .splittingMemberUserUuids(Collections.singletonList("user-uuid-son"))
                .build();

        Family childFamily = Family.builder()
                .familyId("FAM-2026-0002")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-son")
                .status(FamilyStatus.ACTIVE)
                .build();

        when(familyService.splitFamily(any())).thenReturn(childFamily);

        mockMvc.perform(post("/v1/family/_split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(splitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0002"));
    }

    @Test
    @DisplayName("POST /v1/family/_merge should return 200 OK")
    void mergeFamily_Endpoint_Success() throws Exception {
        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .requestInfo(requestInfo)
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-2026-0001")
                .mergingFamilyId("FAM-2026-0002")
                .commonHeadUserUuid("user-uuid-head")
                .build();

        when(familyService.mergeFamily(any())).thenReturn(family);

        mockMvc.perform(post("/v1/family/_merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }
}
