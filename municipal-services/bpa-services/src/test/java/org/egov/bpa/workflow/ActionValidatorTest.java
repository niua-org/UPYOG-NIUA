package org.egov.bpa.workflow;

import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.State;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionValidatorTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ActionValidator actionValidator;

    @Test
    void validateUpdateRequest_whenRoleCannotPerformAction_throwsUnauthorized() {
        State state = State.builder()
                .applicationStatus("INPROGRESS")
                .actions(List.of(Action.builder().action("APPROVE").roles(List.of("BPA_APPROVER")).build()))
                .build();
        BusinessService businessService = BusinessService.builder().states(List.of(state)).build();
        when(workflowService.getCurrentStateObj(eq("INPROGRESS"), any())).thenReturn(state);

        BPARequest request = bpaRequest("FORWARD", "INPROGRESS", List.of(Role.builder().code("BPA_CREATOR").build()));

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(request, businessService))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("The action cannot be performed by this user");
    }

    @Test
    void validateUpdateRequest_whenRoleMatchesAction_passes() {
        State state = State.builder()
                .applicationStatus("INPROGRESS")
                .actions(List.of(Action.builder().action("APPROVE").roles(List.of("BPA_APPROVER")).build()))
                .build();
        BusinessService businessService = BusinessService.builder().states(List.of(state)).build();
        when(workflowService.getCurrentStateObj(eq("INPROGRESS"), any())).thenReturn(state);
        when(workflowService.isStateUpdatable(eq("INPROGRESS"), any())).thenReturn(true);

        BPARequest request = bpaRequest("APPROVE", "INPROGRESS", List.of(Role.builder().code("BPA_APPROVER").build()));

        assertThatCode(() -> actionValidator.validateUpdateRequest(request, businessService)).doesNotThrowAnyException();
    }

    @Test
    void validateUpdateRequest_whenStateNotUpdatableAndDocumentMissingId_throwsInvalidUpdate() {
        State state = State.builder()
                .applicationStatus("APPROVED")
                .actions(List.of(Action.builder().action("APPROVE").roles(List.of("BPA_APPROVER")).build()))
                .build();
        BusinessService businessService = BusinessService.builder().states(List.of(state)).build();
        when(workflowService.getCurrentStateObj(eq("APPROVED"), any())).thenReturn(state);
        when(workflowService.isStateUpdatable(eq("APPROVED"), any())).thenReturn(false);

        BPA bpa = BPA.builder().status("APPROVED").build();
        bpa.setWorkflow(Workflow.builder().action("APPROVE").build());
        bpa.setDocuments(List.of(Document.builder().documentType("PLAN").build()));
        BPARequest request = BPARequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().roles(List.of(Role.builder().code("BPA_APPROVER").build())).build()).build())
                .BPA(bpa)
                .build();

        assertThatThrownBy(() -> actionValidator.validateUpdateRequest(request, businessService))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(BPAConstants.INVALID_UPDATE);
    }

    private static BPARequest bpaRequest(String action, String status, List<Role> roles) {
        BPA bpa = BPA.builder().status(status).build();
        bpa.setWorkflow(Workflow.builder().action(action).build());
        return BPARequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().roles(roles).build()).build())
                .BPA(bpa)
                .build();
    }
}
