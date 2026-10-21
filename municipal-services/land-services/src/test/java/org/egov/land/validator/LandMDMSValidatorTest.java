package org.egov.land.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LandMDMSValidatorTest {

    @InjectMocks
    private LandMDMSValidator validator;

    @Test
    @DisplayName("getAttributeValues_whenInvalidMdmsPayload_throwsCustomException")
    void getAttributeValues_whenInvalidMdmsPayload_throwsCustomException() {
        assertThatThrownBy(() -> validator.getAttributeValues("{}"))
                .isInstanceOf(CustomException.class);
    }
}
