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
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.FamilyMemberService;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.advice.GlobalExceptionHandler;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FamilyMemberControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FamilyMemberService memberService;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private FamilyMemberController memberController;

    private Family family;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .build();

        org.mockito.Mockito.lenient().when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(new ResponseInfo());
    }

    @Test
    @DisplayName("POST /v1/_addMember should return HTTP 200 OK")
    void addMember_Success() throws Exception {
        FamilyMember member = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .relationshipWithHead("SPOUSE")
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(member)
                .consentToken("CONSENT-VERIFIED-12345")
                .build();

        when(memberService.addMember(any(AddMemberRequest.class))).thenReturn(family);

        mockMvc.perform(post("/v1/_addMember")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }

    @Test
    @DisplayName("POST /v1/_joinFamily should return HTTP 200 OK")
    void joinFamily_Success() throws Exception {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-3")
                .relationshipWithHead("DAUGHTER")
                .hofConsentOtp("OTP-1234")
                .build();

        when(memberService.joinFamily(any(JoinFamilyRequest.class))).thenReturn(family);

        mockMvc.perform(post("/v1/_joinFamily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }

    @Test
    @DisplayName("POST /v1/_leaveFamily should return HTTP 200 OK")
    void leaveFamily_Success() throws Exception {
        LeaveFamilyRequest request = LeaveFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .memberUserUuid("member-uuid-2")
                .exitReason(ExitReason.MARRIAGE)
                .build();

        when(memberService.leaveFamily(any(LeaveFamilyRequest.class))).thenReturn(family);

        mockMvc.perform(post("/v1/_leaveFamily")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Families[0].familyId").value("FAM-2026-0001"));
    }
}
