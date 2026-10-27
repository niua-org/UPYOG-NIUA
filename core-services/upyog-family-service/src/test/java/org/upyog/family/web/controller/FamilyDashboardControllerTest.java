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
        FamilyDashboardMetrics metrics = FamilyDashboardMetrics.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .totalMembersCount(3)
                .activeMembersCount(3)
                .verifiedMembersCount(2)
                .documentsCount(1)
                .lineageEventsCount(0)
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
                .andExpect(jsonPath("$.dashboard.totalMembersCount").value(3))
                .andExpect(jsonPath("$.dashboard.activeMembersCount").value(3));
    }
}
