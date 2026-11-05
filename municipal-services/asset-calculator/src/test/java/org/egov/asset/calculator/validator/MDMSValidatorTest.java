package org.egov.asset.calculator.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @InjectMocks
    private MDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidPayload_throws() {
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateMdmsData_whenMasterMissing_throws() {
        Map<String, Object> mdms = Map.of("MdmsRes", Map.of("FSM", Map.of()));
        assertThatThrownBy(() -> validator.validateMdmsData(mdms))
                .isInstanceOf(CustomException.class);
    }
}
