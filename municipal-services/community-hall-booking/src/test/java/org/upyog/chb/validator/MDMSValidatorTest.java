package org.upyog.chb.validator;

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
    void getAttributeValues_whenMdmsPayloadValid_returnsMasterMap() {
        Map<String, Object> mdmsRoot = Map.of("MdmsRes", Map.of("CHB", Map.of("CHBPurpose", List.of("Wedding"))));

        Map<String, List<String>> attributes = mdmsValidator.getAttributeValues(mdmsRoot);

        assertThat(attributes).containsKey("CHBPurpose");
    }

    @Test
    void getAttributeValues_whenJsonPathMissing_throwsCustomException() {
        assertThatThrownBy(() -> mdmsValidator.getAttributeValues(new HashMap<>()))
                .isInstanceOf(CustomException.class);
    }
}
