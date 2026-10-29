package org.egov.vendor.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @InjectMocks
    private MDMSValidator validator;

    @Test
    @DisplayName("getAttributeValues_whenInvalidMdmsPayload_throwsCustomException")
    void getAttributeValues_whenInvalidMdmsPayload_throwsCustomException() {
        assertThatThrownBy(() -> validator.getAttributeValues("{}"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getAttributeValues_whenValidPayload_returnsMasterEntries() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of("Vendor", Map.of("AgencyType", List.of("PRIVATE"))));

        Map<String, List<String>> attributes = validator.getAttributeValues(mdms);

        assertThat(attributes).containsKey("AgencyType");
    }
}
