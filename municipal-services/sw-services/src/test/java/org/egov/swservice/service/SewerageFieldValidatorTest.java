package org.egov.swservice.service;

import org.egov.swservice.util.SWConstants;
import org.egov.swservice.web.models.RoadCuttingInfo;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.ValidatorResult;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SewerageFieldValidatorTest {

    private final SewerageFieldValidator validator = new SewerageFieldValidator();

    @Test
    void validateDisconnectionRequest_whenExecuteWithoutConnectionType_fails() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setProcessInstance(ProcessInstance.builder().action(SWConstants.EXECUTE_DISCONNECTION).build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(connection).build();

        ValidatorResult result = validator.validate(request, SWConstants.DISCONNECT_CONNECTION);

        assertThat(result.isStatus()).isFalse();
        assertThat(result.getErrorMessage()).containsKey("INVALID_SEWERAGE_CONNECTION_TYPE");
    }

    @Test
    void validateUpdateRequest_whenActivateWithoutExecutionDate_fails() {
        SewerageConnection connection = SewerageConnection.builder().connectionType("NON_METERED").build();
        connection.setProcessInstance(ProcessInstance.builder().action(SWConstants.ACTIVATE_CONNECTION_CONST).build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(connection).build();

        ValidatorResult result = validator.validate(request, SWConstants.UPDATE_APPLICATION);

        assertThat(result.isStatus()).isFalse();
        assertThat(result.getErrorMessage()).containsKey("INVALID_CONNECTION_EXECUTION_DATE");
    }

    @Test
    void validateUpdateRequest_whenApproveWithoutRoadInfo_fails() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setProcessInstance(ProcessInstance.builder().action(SWConstants.APPROVE_CONNECTION_CONST).build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(connection).build();

        ValidatorResult result = validator.validate(request, SWConstants.UPDATE_APPLICATION);

        assertThat(result.isStatus()).isFalse();
        assertThat(result.getErrorMessage()).containsKey("INVALID_ROAD_INFO");
    }

    @Test
    void validateUpdateRequest_whenApproveWithIncompleteRoadCutting_fails() {
        SewerageConnection connection = SewerageConnection.builder()
                .roadCuttingInfo(List.of(RoadCuttingInfo.builder().roadType("").build()))
                .build();
        connection.setProcessInstance(ProcessInstance.builder().action(SWConstants.APPROVE_CONNECTION_CONST).build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(connection).build();

        ValidatorResult result = validator.validate(request, SWConstants.UPDATE_APPLICATION);

        assertThat(result.isStatus()).isFalse();
        assertThat(result.getErrorMessage()).containsKey("INVALID_ROAD_TYPE");
    }
}
