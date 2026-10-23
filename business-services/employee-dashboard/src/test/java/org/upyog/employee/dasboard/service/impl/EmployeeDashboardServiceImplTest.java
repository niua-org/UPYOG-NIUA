package org.upyog.employee.dasboard.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.employee.dasboard.query.constant.DashboardConstants;
import org.upyog.employee.dasboard.repository.impl.ServiceRequestRepositoryImpl;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardDetails;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardRequest;
import org.upyog.employee.dasboard.web.models.ModuleName;
import org.upyog.employee.dasboard.web.models.RoleBasedDashboardRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardServiceImplTest {

    @Mock
    private ServiceRequestRepositoryImpl dashboardRepository;

    @InjectMocks
    private EmployeeDashboardServiceImpl service;

    @Test
    void getEmployeeDashboardData_returnsResponseWithDetails() {
        EmployeeDashboardDetails details = new EmployeeDashboardDetails();
        details.setApplicationReceived(10);
        when(dashboardRepository.fetchModuleData(any())).thenReturn(details);

        EmployeeDashboardRequest request = new EmployeeDashboardRequest();
        request.setModuleName(ModuleName.PGR);
        request.setTenantId("pb.amritsar");
        request.setRequestInfo(RequestInfo.builder().apiId("dash").build());

        var response = service.getEmployeeDashboardData(request);

        assertThat(response.getEmployeeDashbaord().getApplicationReceived()).isEqualTo(10);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo(org.upyog.employee.dasboard.web.models.ResponseInfo.StatusEnum.SUCCESSFUL);
    }

    @Test
    void getRoleBasedDashboardData_mapsRolesToModules() {
        EmployeeDashboardDetails details = new EmployeeDashboardDetails();
        details.setApplicationPending(2);
        when(dashboardRepository.fetchModuleData(any())).thenReturn(details);

        Role role = Role.builder().code("PGR_DASHBOARD_VIEWER").build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().roles(Collections.singletonList(role)).build())
                .build();
        RoleBasedDashboardRequest request = new RoleBasedDashboardRequest();
        request.setRequestInfo(requestInfo);
        request.setTenantId("pb.amritsar");

        var response = service.getRoleBasedDashboardData(request);

        assertThat(response.getDashboardData()).containsKey(DashboardConstants.ROLE_TO_MODULE_MAP.get("PGR_DASHBOARD_VIEWER"));
    }
}
