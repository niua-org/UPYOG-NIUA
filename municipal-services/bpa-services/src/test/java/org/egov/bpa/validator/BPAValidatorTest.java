package org.egov.bpa.validator;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BPAValidatorTest {

    @Mock
    private BPAConfiguration config;

    @InjectMocks
    private BPAValidator bpaValidator;

    @Test
    void validateSearch_whenEmployeeEmptyCriteria_throwsInvalidSearch() {
        RequestInfo requestInfo = employeeRequest();

        assertThatThrownBy(() -> bpaValidator.validateSearch(requestInfo, BPASearchCriteria.builder().build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Search without any paramters is not allowed");
    }

    @Test
    void validateSearch_whenEmployeeMissingTenantOnNonTenantOnlySearch_throws() {
        RequestInfo requestInfo = employeeRequest();
        BPASearchCriteria criteria = BPASearchCriteria.builder().applicationNo("BPA-001").build();

        assertThatThrownBy(() -> bpaValidator.validateSearch(requestInfo, criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("TenantId is mandatory in search");
    }

    @Test
    void validateSearch_whenEmployeeAllowedParams_passes() {
        RequestInfo requestInfo = employeeRequest();
        when(config.getAllowedEmployeeSearchParameters()).thenReturn("tenantId,applicationNo,status");

        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").applicationNo("BPA-001").build();

        assertThatCode(() -> bpaValidator.validateSearch(requestInfo, criteria)).doesNotThrowAnyException();
    }

    @Test
    void validateSearch_whenDisallowedParam_throwsInvalidSearch() {
        RequestInfo requestInfo = employeeRequest();
        when(config.getAllowedEmployeeSearchParameters()).thenReturn("tenantId,status");

        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").applicationNo("BPA-001").build();

        assertThatThrownBy(() -> bpaValidator.validateSearch(requestInfo, criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Search on applicationNo is not allowed");
    }

    @Test
    void validateSearch_whenUnknownUserType_throws() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("SYSTEM").build())
                .build();

        assertThatThrownBy(() -> bpaValidator.validateSearch(requestInfo, BPASearchCriteria.builder().tenantId("pb").build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("does not have any search config");
    }

    private static RequestInfo employeeRequest() {
        return RequestInfo.builder().userInfo(User.builder().type(BPAConstants.EMPLOYEE).build()).build();
    }
}
