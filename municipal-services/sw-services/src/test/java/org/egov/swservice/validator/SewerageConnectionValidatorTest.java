package org.egov.swservice.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.swservice.service.PropertyValidator;
import org.egov.swservice.service.SewerageFieldValidator;
import org.egov.swservice.util.SWConstants;
import org.egov.swservice.web.models.Document;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SewerageConnectionValidatorTest {

    private final SewerageConnectionValidator validator = new SewerageConnectionValidator();

    @BeforeEach
    void wireValidators() {
        ReflectionTestUtils.setField(validator, "propertyValidator", new PropertyValidator());
        ReflectionTestUtils.setField(validator, "sewerageFieldValidator", new SewerageFieldValidator());
    }

    @Test
    void validateSewerageConnection_whenActionMissing_throwsInvalidAction() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setPropertyId("p1");
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().type("CITIZEN").build()).build())
                .sewerageConnection(connection)
                .build();

        assertThatThrownBy(() -> validator.validateSewerageConnection(request, SWConstants.CREATE_APPLICATION))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID_ACTION");
    }

    @Test
    void validateSewerageConnection_whenPayAction_throwsInvalidAction() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setPropertyId("p1");
        connection.setProcessInstance(ProcessInstance.builder().action("PAY").build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().type("CITIZEN").build()).build())
                .sewerageConnection(connection)
                .build();

        assertThatThrownBy(() -> validator.validateSewerageConnection(request, SWConstants.CREATE_APPLICATION))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Pay action cannot perform directly");
    }

    @Test
    void validateSewerageConnection_whenEmployeeUsesCitizenChannel_throwsInvalidChannel() {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setPropertyId("p1");
        connection.setChannel("CITIZEN");
        connection.setProcessInstance(ProcessInstance.builder().action("APPLY").build());
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().type("EMPLOYEE").build()).build())
                .sewerageConnection(connection)
                .build();

        assertThatThrownBy(() -> validator.validateSewerageConnection(request, SWConstants.CREATE_APPLICATION))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID_CHANNEL");
    }

    @Test
    void validateUpdate_whenApplicationNoMismatch_throwsInvalidUpdate() {
        SewerageConnection update = SewerageConnection.builder().build();
        update.setApplicationNo("SW-NEW");
        SewerageConnection search = SewerageConnection.builder().build();
        search.setApplicationNo("SW-OLD");
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(update).build();

        assertThatThrownBy(() -> validator.validateUpdate(request, search))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void validateUpdate_whenDuplicateDocuments_throwsDuplicateDocument() {
        SewerageConnection update = SewerageConnection.builder().build();
        update.setApplicationNo("SW-1");
        update.setDocuments(List.of(
                Document.builder().fileStoreId("fs-1").build(),
                Document.builder().fileStoreId("fs-1").build()));
        SewerageConnection search = SewerageConnection.builder().build();
        search.setApplicationNo("SW-1");
        SewerageConnectionRequest request = SewerageConnectionRequest.builder().sewerageConnection(update).build();

        assertThatThrownBy(() -> validator.validateUpdate(request, search))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Same document cannot be used multiple times");
    }
}
