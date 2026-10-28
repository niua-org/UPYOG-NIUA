package org.egov.swservice.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.web.models.Property;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.Status;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatePropertyTest {

    private final ValidateProperty validateProperty = new ValidateProperty();

    @Test
    void validatePropertyFields_whenTenantIdMissing_throws() {
        Property property = Property.builder().status(Status.ACTIVE).build();

        assertThatThrownBy(() -> validateProperty.validatePropertyFields(property, RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID_PROPERTY");
    }

    @Test
    void validatePropertyForConnection_whenPropertyIdMissing_logsErrorWithoutThrow() {
        SewerageConnection connection = SewerageConnection.builder().applicationNo("SW-APP-1").build();

        validateProperty.validatePropertyForConnection(List.of(connection));
    }
}
