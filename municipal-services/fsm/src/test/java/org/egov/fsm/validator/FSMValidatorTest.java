package org.egov.fsm.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.util.FSMConstants;
import org.egov.fsm.web.model.FSMSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FSMValidatorTest {

    @Mock private MDMSValidator mdmsValidator;
    @Mock private FSMConfiguration config;

    @InjectMocks
    private FSMValidator fsmValidator;

    @Test
    void validateSearch_whenUnknownUserType_throwsInvalidSearch() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("ANONYMOUS").build())
                .build();
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb.amritsar").build();

        assertThatThrownBy(() -> fsmValidator.validateSearch(requestInfo, criteria))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateSearch_whenEmployeeAtStateLevel_throws() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type(FSMConstants.EMPLOYEE).build())
                .build();
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb").applicationNos(java.util.List.of("FSM-1")).build();

        assertThatThrownBy(() -> fsmValidator.validateSearch(requestInfo, criteria))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateSearch_whenCitizenWithAllowedParams_passes() {
        when(config.getAllowedCitizenSearchParameters())
                .thenReturn("tenantId,applicationStatus,locality,ownerIds,applicationNos");
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type(FSMConstants.CITIZEN).build())
                .build();
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb.amritsar").build();

        fsmValidator.validateSearch(requestInfo, criteria);
    }
}
