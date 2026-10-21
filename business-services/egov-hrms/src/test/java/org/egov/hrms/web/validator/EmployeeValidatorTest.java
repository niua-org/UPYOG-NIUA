package org.egov.hrms.web.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.service.EmployeeService;
import org.egov.hrms.service.MDMSService;
import org.egov.hrms.service.UserService;
import org.egov.hrms.utils.ErrorConstants;
import org.egov.hrms.web.contract.EmployeeSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeValidatorTest {

    @Mock
    private MDMSService mdmsService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private UserService userService;
    @Mock
    private PropertiesManager propertiesManager;

    @InjectMocks
    private EmployeeValidator employeeValidator;

    @Test
    void validateEmployeeCountRequest_blankTenantId_throws() {
        assertThatThrownBy(() -> employeeValidator.validateEmployeeCountRequest(null))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrors()).containsKey(ErrorConstants.HRMS_EMPLOYEE_COUNT_ERROR_CODE);
                });
    }

    @Test
    void validateEmployeeCountRequest_validTenantId_passes() {
        employeeValidator.validateEmployeeCountRequest("pb.amritsar");
    }

    @Test
    void validateSearchRequest_citizenWithIds_throws() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type(ErrorConstants.CITIZEN_TYPE_CODE).build())
                .build();
        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .ids(List.of(1L))
                .tenantId("pb.amritsar")
                .build();

        assertThatThrownBy(() -> employeeValidator.validateSearchRequest(requestInfo, criteria))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateSearchRequest_openSearchWithoutRole_throws() {
        when(propertiesManager.getOpenSearchEnabledRoles()).thenReturn("SUPERUSER");
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder()
                        .type("EMPLOYEE")
                        .roles(Collections.singletonList(Role.builder().code("HRMS_ADMIN").build()))
                        .build())
                .build();
        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder().build();

        assertThatThrownBy(() -> employeeValidator.validateSearchRequest(requestInfo, criteria))
                .isInstanceOf(CustomException.class);
    }
}
