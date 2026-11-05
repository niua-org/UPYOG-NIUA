package org.upyog.family.web.controller;

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
import org.upyog.family.domain.service.FamilyDashboardService;
import org.upyog.family.web.advice.GlobalExceptionHandler;
import org.upyog.family.web.contract.FamilyDashboardMetrics;
import org.upyog.family.web.contract.FamilyDashboardResponse;
import org.upyog.family.web.contract.FamilyMember;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FamilyDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FamilyDashboardService dashboardService;

    @InjectMocks
    private FamilyDashboardController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /v1/dashboard/_search should return HTTP 200 OK with dashboard metrics")
    void getDashboard_Success() throws Exception {
        FamilyMember member = FamilyMember.builder()
                .userUuid("user-uuid-1")
                .name("Rajesh Sharma")
                .photo("photo-123")
                .relationshipWithHead("HEAD")
                .mobileNumber("9876543210")
                .gender("MALE")
                .dob(631152000000L)
                .isHead(true)
                .active(true)
                .build();

        FamilyDashboardMetrics metrics = FamilyDashboardMetrics.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .headName("Rajesh Sharma")
                .headUserUuid("user-uuid-1")
                .totalMembersCount(1)
                .activeMembersCount(1)
                .verifiedMembersCount(1)
                .documentsCount(1)
                .lineageEventsCount(0)
                .activeMembers(Collections.singletonList(member))
                .build();

        FamilyDashboardResponse response = FamilyDashboardResponse.builder()
                .responseInfo(new ResponseInfo())
                .dashboard(metrics)
                .build();

        when(dashboardService.getFamilyDashboard(eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(response);

        mockMvc.perform(post("/v1/dashboard/_search")
                        .param("familyId", "FAM-2026-0001")
                        .param("tenantId", "pg.citya")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboard.familyId").value("FAM-2026-0001"))
                .andExpect(jsonPath("$.dashboard.headName").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.dashboard.totalMembersCount").value(1))
                .andExpect(jsonPath("$.dashboard.activeMembersCount").value(1))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].name").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].photo").value("photo-123"))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].relationshipWithHead").value("HEAD"))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].mobileNumber").value("9876543210"))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].gender").value("MALE"))
                .andExpect(jsonPath("$.dashboard.activeMembers[0].dob").value(631152000000L));
    }
}
