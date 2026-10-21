package org.upyog.employee.dasboard.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.employee.dasboard.service.EmployeeDashboardService;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardRequest;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardResponse;
import org.upyog.employee.dasboard.web.models.ModuleName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardApiControllerWebTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EmployeeDashboardService dashboardService;

    @InjectMocks
    private EmployeeDashaboardApiController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void search_returnsOk() throws Exception {
        when(dashboardService.getEmployeeDashboardData(any()))
                .thenReturn(EmployeeDashboardResponse.builder().build());

        EmployeeDashboardRequest request = new EmployeeDashboardRequest();
        request.setRequestInfo(RequestInfo.builder().apiId("dashboard").build());
        request.setTenantId("pb.amritsar");
        request.setModuleName(ModuleName.PGR);

        mockMvc.perform(post("/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
