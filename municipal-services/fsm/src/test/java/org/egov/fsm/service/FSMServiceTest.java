package org.egov.fsm.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.fsm.config.FSMConfiguration;
import org.egov.fsm.fsmProducer.FSMProducer;
import org.egov.fsm.repository.FSMRepository;
import org.egov.fsm.service.notification.NotificationService;
import org.egov.fsm.util.FSMUtil;
import org.egov.fsm.validator.FSMValidator;
import org.egov.fsm.web.model.FSM;
import org.egov.fsm.web.model.FSMRequest;
import org.egov.fsm.web.model.FSMResponse;
import org.egov.fsm.web.model.FSMSearchCriteria;
import org.egov.fsm.web.model.Workflow;
import org.egov.fsm.web.model.user.UserDetailResponse;
import org.egov.fsm.workflow.ActionValidator;
import org.egov.fsm.workflow.WorkflowIntegrator;
import org.egov.fsm.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FSMServiceTest {

    @Mock private FSMUtil util;
    @Mock private EnrichmentService enrichmentService;
    @Mock private FSMValidator fsmValidator;
    @Mock private WorkflowIntegrator wfIntegrator;
    @Mock private ActionValidator actionValidator;
    @Mock private WorkflowService workflowService;
    @Mock private UserService userService;
    @Mock private VehicleTripService vehicleTripService;
    @Mock private CalculationService calculationService;
    @Mock private DSOService dsoService;
    @Mock private FSMConfiguration config;
    @Mock private VehicleService vehicleService;
    @Mock private FSMProducer producer;
    @Mock private FSMRepository repository;
    @Mock private NotificationService notificationService;
    @Mock private BillingService billingService;
    @Mock private FSMInboxService fSMInboxService;

    @InjectMocks
    private FSMService fsmService;

    @Test
    void create_whenStateLevelTenant_throwsBeforePersist() {
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder().userInfo(User.builder().type("CITIZEN").build()).build())
                .fsm(FSM.builder().tenantId("pb").build())
                .build();
        when(util.mDMSCall(any(), eq("pb"))).thenReturn(Collections.emptyMap());

        assertThatThrownBy(() -> fsmService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("StateLevel");
        verify(repository, never()).save(any());
    }

    @Test
    void update_whenApplicationIdMissing_throws() {
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder().build())
                .fsm(FSM.builder().tenantId("pb.amritsar").build())
                .workflow(Workflow.builder().action("SUBMIT").build())
                .build();
        when(util.mDMSCall(any(), any())).thenReturn(Collections.emptyMap());

        assertThatThrownBy(() -> fsmService.update(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Application Not found");
    }

    @Test
    void fsmSearch_whenMobileLookupEmpty_returnsEmptyResponse() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("EMPLOYEE").build())
                .build();
        FSMSearchCriteria criteria = FSMSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .mobileNumber("9999999999")
                .applicationNos(List.of("FSM-1"))
                .build();
        when(userService.getUser(criteria, requestInfo))
                .thenReturn(new UserDetailResponse(null, Collections.emptyList()));

        FSMResponse response = fsmService.fsmSearch(criteria, requestInfo);

        assertThat(response.getFsm()).isEmpty();
        verify(repository, never()).getFSMData(any(), any());
    }

    @Test
    void searchFSMPlainSearch_whenNoIds_returnsEmptyList() {
        FSMSearchCriteria criteria = FSMSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(config.getMaxSearchLimit()).thenReturn(100);
        when(repository.fetchFSMIds(criteria)).thenReturn(Collections.emptyList());

        assertThat(fsmService.searchFSMPlainSearch(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
