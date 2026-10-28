package org.egov.ndc.validator;

import org.egov.ndc.util.NDCConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MDMSValidatorTest {

    private final MDMSValidator mdmsValidator = new MDMSValidator();

    @Test
    void getAttributeValues_whenMdmsPayloadValid_mergesModuleMaps() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of(
                        "NDC", Map.of(NDCConstants.NDC_TYPE, List.of("RESIDENTIAL")),
                        "common-masters", Map.of(NDCConstants.DOCUMENT_TYPE, List.of("AADHAAR"))));

        Map<String, List<String>> attributes = mdmsValidator.getAttributeValues(mdms);

        assertThat(attributes).containsKeys(NDCConstants.NDC_TYPE, NDCConstants.DOCUMENT_TYPE);
    }

    @Test
    void getAttributeValues_whenJsonPathMissing_throwsCustomException() {
        assertThatThrownBy(() -> mdmsValidator.getAttributeValues(new HashMap<>()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateMdmsData_whenMasterMissing_throwsCustomException() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of("NDC", Map.of(), "common-masters", Map.of()));

        assertThatThrownBy(() -> mdmsValidator.validateMdmsData(mdms))
                .isInstanceOf(CustomException.class);
    }
}
