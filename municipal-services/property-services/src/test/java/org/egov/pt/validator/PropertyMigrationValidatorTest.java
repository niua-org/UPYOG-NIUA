package org.egov.pt.validator;

import org.egov.pt.models.Property;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PropertyMigrationValidatorTest {

    @InjectMocks
    private PropertyMigrationValidator propertyMigrationValidator;

    @Test
    void validatePropertyCreateRequest_whenOwnersEmpty_throwsOwnerInfoError() {
        Property property = Property.builder()
                .owners(new ArrayList<>())
                .creationReason(CreationReason.CREATE)
                .build();
        PropertyRequest request = PropertyRequest.builder().property(property).build();

        assertThatThrownBy(() -> propertyMigrationValidator.validatePropertyCreateRequest(request, Map.of(), new HashMap<>()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Owners cannot be empty");
    }

}
