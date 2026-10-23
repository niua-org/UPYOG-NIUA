package org.egov.pt.validator;

import org.egov.pt.models.Property;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PropertyValidatorCreateTest {

    @InjectMocks
    private PropertyValidator propertyValidator;

    @Test
    @DisplayName("validateCreateRequest_whenOwnersEmpty_throwsOwnerInfoError")
    void validateCreateRequest_whenOwnersEmpty_throwsOwnerInfoError() {
        Property property = Property.builder()
                .owners(Collections.emptyList())
                .build();
        PropertyRequest request = PropertyRequest.builder().property(property).build();

        assertThatThrownBy(() -> propertyValidator.validateCreateRequest(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Owners cannot be empty");
    }
}
