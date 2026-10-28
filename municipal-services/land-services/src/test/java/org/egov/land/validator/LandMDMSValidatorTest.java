package org.egov.land.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LandMDMSValidatorTest {

    @InjectMocks
    private LandMDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidPayload_throws() {
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }
}
