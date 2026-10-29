package org.egov.swservice.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.util.SWConstants;
import org.egov.swservice.web.models.Document;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.workflow.BusinessService;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.egov.swservice.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    void validateUpdateRequest_whenInitiateWithDocuments_throwsInvalidStatus() {
        ProcessInstance pi = ProcessInstance.builder().action(SWConstants.ACTION_INITIATE).build();
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setProcessInstance(pi);
        connection.setDocuments(List.of(Document.builder().documentType("OWNERID").build()));
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(connection)
                .build();

        assertThatThrownBy(
                        () -> actionValidator.validateUpdateRequest(request, BusinessService.builder().build(), "OPEN"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Status cannot be INITIATE");
    }

    @Test
    void validateUpdateRequest_whenStateNotUpdatableAndMissingIds_throwsInvalidUpdate() {
        when(workflowService.isStateUpdatable(eq("LOCKED"), any())).thenReturn(false);
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setProcessInstance(ProcessInstance.builder().action("APPROVE").build());
        connection.setDocuments(List.of(Document.builder().documentType("A").build()));
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(connection)
                .build();

        assertThatThrownBy(
                        () -> actionValidator.validateUpdateRequest(request, BusinessService.builder().build(), "LOCKED"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateUpdateRequest_whenStateUpdatableAndMissingIds_passes() {
        when(workflowService.isStateUpdatable(eq("OPEN"), any())).thenReturn(true);
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setProcessInstance(ProcessInstance.builder().action("APPROVE").build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(connection)
                .build();

        actionValidator.validateUpdateRequest(request, BusinessService.builder().build(), "OPEN");
    }
}
