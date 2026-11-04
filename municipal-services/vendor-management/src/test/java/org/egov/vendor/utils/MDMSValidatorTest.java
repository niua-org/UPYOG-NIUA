package org.egov.vendor.utils;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @InjectMocks
    private MDMSValidator mdmsValidator;

    @Test
    @DisplayName("getAttributeValues_whenInvalidJsonPath_throwsCustomException")
    void getAttributeValues_whenInvalidJsonPath_throwsCustomException() {
        assertThatThrownBy(() -> mdmsValidator.getAttributeValues("{}"))
                .isInstanceOf(CustomException.class);
    }
}
