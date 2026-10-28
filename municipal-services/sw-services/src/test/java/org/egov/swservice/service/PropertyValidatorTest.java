package org.egov.swservice.service;

import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.swservice.web.models.ValidatorResult;
import org.egov.swservice.util.SWConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyValidatorTest {

    private final PropertyValidator propertyValidator = new PropertyValidator();

    @Test
    void validate_whenPropertyIdMissing_returnsError() {
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(SewerageConnection.builder().build())
                .build();

        ValidatorResult result = propertyValidator.validate(request, SWConstants.CREATE_APPLICATION);

        assertThat(result.isStatus()).isFalse();
        assertThat(result.getErrorMessage()).containsKey("INVALID_PROPERTY_UNIQUE_ID");
    }

    @Test
    void validate_whenPropertyIdPresent_returnsSuccess() {
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .sewerageConnection(SewerageConnection.builder().propertyId("prop-1").build())
                .build();

        ValidatorResult result = propertyValidator.validate(request, SWConstants.CREATE_APPLICATION);

        assertThat(result.isStatus()).isTrue();
        assertThat(result.getErrorMessage()).isEmpty();
    }
}
