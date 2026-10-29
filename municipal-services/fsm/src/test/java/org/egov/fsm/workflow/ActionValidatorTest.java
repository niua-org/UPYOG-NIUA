package org.egov.fsm.workflow;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.fsm.web.model.FSM;
import org.egov.fsm.web.model.FSMRequest;
import org.egov.fsm.web.model.Workflow;
import org.egov.fsm.web.model.workflow.Action;
import org.egov.fsm.web.model.workflow.BusinessService;
import org.egov.fsm.web.model.workflow.State;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionValidatorTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ActionValidator actionValidator;

    @Test
    void validateUpdateRequest_whenActionNotAllowedForRole_throwsUnauthorized() {
        FSM fsm = FSM.builder().applicationStatus("OPEN").id("fsm-1").build();
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder()
                        .userInfo(User.builder()
                                .roles(List.of(Role.builder().code("CITIZEN").build()))
                                .build())
                        .build())
                .fsm(fsm)
                .workflow(Workflow.builder().action("APPROVE").build())
                .build();
        BusinessService businessService = BusinessService.builder()
                .states(List.of(State.builder()
                        .applicationStatus("OPEN")
                        .actions(List.of(Action.builder()
                                .action("SUBMIT")
                                .roles(List.of("EMPLOYEE"))
                                .build()))
                        .build()))
                .build();
        when(workflowService.getCurrentStateObj("OPEN", businessService))
                .thenReturn(businessService.getStates().get(0));

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(request, businessService))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateUpdateRequest_whenMatchingRoleAndAction_passes() {
        FSM fsm = FSM.builder().applicationStatus("OPEN").id("fsm-1").build();
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder()
                        .userInfo(User.builder()
                                .roles(List.of(Role.builder().code("EMPLOYEE").build()))
                                .build())
                        .build())
                .fsm(fsm)
                .workflow(Workflow.builder().action("SUBMIT").build())
                .build();
        BusinessService businessService = BusinessService.builder()
                .states(List.of(State.builder()
                        .applicationStatus("OPEN")
                        .actions(List.of(Action.builder()
                                .action("SUBMIT")
                                .roles(List.of("EMPLOYEE"))
                                .build()))
                        .build()))
                .build();
        when(workflowService.getCurrentStateObj("OPEN", businessService))
                .thenReturn(businessService.getStates().get(0));
        when(workflowService.isStateUpdatable("OPEN", businessService)).thenReturn(true);

        actionValidator.validateUpdateRequest(request, businessService);
    }
}
