package org.egov.bpa.validator;

import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPARequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
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
    void validateMdmsData_whenRequiredMastersPresent_passes() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of(
                        "BPA",
                        Map.of(
                                BPAConstants.SERVICE_TYPE, List.of(Map.of("code", "NEW")),
                                BPAConstants.APPLICATION_TYPE, List.of(Map.of("code", "BUILDING_PLAN")),
                                BPAConstants.OCCUPANCY_TYPE, List.of(Map.of("code", "RES")),
                                BPAConstants.SUB_OCCUPANCY_TYPE, List.of(Map.of("code", "A")),
                                BPAConstants.USAGES, List.of(Map.of("code", "U"))),
                        "common-masters",
                        Map.of(
                                BPAConstants.OWNERSHIP_CATEGORY, List.of(Map.of("code", "OWN")),
                                BPAConstants.OWNER_TYPE, List.of(Map.of("code", "IND")))));

        assertThatCode(() -> validator.validateMdmsData(BPARequest.builder().build(), mdms)).doesNotThrowAnyException();
    }

    @Test
    void validateMdmsData_whenMasterMissing_throwsMdmsError() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of(
                        "BPA",
                        Map.of(BPAConstants.SERVICE_TYPE, List.of(Map.of("code", "NEW"))),
                        "common-masters",
                        Map.of()));

        assertThatThrownBy(() -> validator.validateMdmsData(BPARequest.builder().build(), mdms))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("MDMS DATA ERROR");
    }
}
