package org.egov.waterconnection.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.web.models.Document;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.BusinessService;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.egov.waterconnection.workflow.WorkflowService;
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
    void validateUpdateRequest_whenInitiateWithDocuments_throwsInvalidStatus() {
        ProcessInstance pi = ProcessInstance.builder().action(WCConstants.ACTION_INITIATE).build();
        WaterConnection wc = WaterConnection.builder().build();
        wc.setProcessInstance(pi);
        wc.setDocuments(List.of(Document.builder().documentType("OWNERID").build()));
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(wc)
                .build();

        assertThatThrownBy(
                        () -> actionValidator.validateUpdateRequest(request, BusinessService.builder().build(), "OPEN"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Status cannot be INITIATE");
    }

    @Test
    void validateUpdateRequest_whenStateNotUpdatableAndMissingIds_throwsInvalidUpdate() {
        when(workflowService.isStateUpdatable(
                        org.mockito.ArgumentMatchers.eq("LOCKED"),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        WaterConnection wc = WaterConnection.builder().build();
        wc.setProcessInstance(ProcessInstance.builder().action("APPROVE").build());
        wc.setDocuments(List.of(Document.builder().documentType("A").build()));
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .waterConnection(wc)
                .build();

        assertThatThrownBy(
                        () -> actionValidator.validateUpdateRequest(request, BusinessService.builder().build(), "LOCKED"))
                .isInstanceOf(CustomException.class);
    }
}
