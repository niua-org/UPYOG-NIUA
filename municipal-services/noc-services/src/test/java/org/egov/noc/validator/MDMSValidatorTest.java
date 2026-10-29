package org.egov.noc.validator;

import org.egov.noc.util.NOCConstants;
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
                        "NOC", Map.of(NOCConstants.NOC_TYPE, List.of("BUILDING")),
                        "common-masters", Map.of(NOCConstants.DOCUMENT_TYPE, List.of("AADHAAR"))));

        Map<String, List<String>> attributes = mdmsValidator.getAttributeValues(mdms);

        assertThat(attributes).containsKeys(NOCConstants.NOC_TYPE, NOCConstants.DOCUMENT_TYPE);
    }

    @Test
    void getAttributeValues_whenJsonPathMissing_throwsCustomException() {
        assertThatThrownBy(() -> mdmsValidator.getAttributeValues(new HashMap<>()))
                .isInstanceOf(CustomException.class);
    }
}
