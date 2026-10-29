package org.egov.wf.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.egov.common.contract.request.Role;
import org.egov.wf.config.WorkflowConfig;
import org.egov.wf.repository.BusinessServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowUtilRoleAvailabilityTest {

    private WorkflowUtil workflowUtil;

    @BeforeEach
    void setUp() {
        workflowUtil = new WorkflowUtil(new ObjectMapper(), mock(WorkflowConfig.class),
                mock(BusinessServiceRepository.class));
    }

    @Test
    void isRoleAvailableMatchesStateLevelTenantAndWildcardActionRole() {
        List<Role> userRoles = List.of(Role.builder().code("EMPLOYEE").tenantId("pb").build());

        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", userRoles, List.of("EMPLOYEE"))).isTrue();
        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", userRoles, List.of("*"))).isTrue();
        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", userRoles, List.of("ADMIN"))).isFalse();
    }

    @Test
    void isRoleAvailableReturnsFalseForMissingUserRolesOrInvalidTenant() {
        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", Collections.emptyList(), List.of("EMPLOYEE"))).isFalse();
        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", null, List.of("EMPLOYEE"))).isFalse();

        List<Role> userRoles = List.of(Role.builder().code("EMPLOYEE").tenantId("other").build());
        assertThat(workflowUtil.isRoleAvailable("pb.amritsar", userRoles, List.of("EMPLOYEE"))).isFalse();
    }

    @Test
    void isRoleAvailableStringListOverloadMatchesCodes() {
        assertThat(workflowUtil.isRoleAvailable(List.of("EMPLOYEE"), List.of("EMPLOYEE"))).isTrue();
        assertThat(workflowUtil.isRoleAvailable(List.of("EMPLOYEE"), List.of("*"))).isTrue();
        assertThat(workflowUtil.isRoleAvailable(Collections.emptyList(), List.of("EMPLOYEE"))).isFalse();
    }
}
