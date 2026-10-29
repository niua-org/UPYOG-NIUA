package com.ingestpipeline.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceImplTest {

    private final ValidationServiceImpl validationService = new ValidationServiceImpl();

    @Test
    void validateData_validTransactionSchema_returnsTrue() {
        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("dataContext", "transaction");
        incomingData.put("dataContextVersion", "v1");
        incomingData.put("dataObject", Map.of(
                "rating", Map.of("primary", Map.of("value", 4))
        ));

        assertThat(validationService.validateData(incomingData)).isTrue();
        // second call exercises schema cache path
        assertThat(validationService.validateData(incomingData)).isTrue();
    }

    @Test
    void validateData_missingRequiredField_returnsFalse() {
        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("dataContext", "transaction");
        incomingData.put("dataContextVersion", "v1");
        incomingData.put("dataObject", Map.of("other", "value"));

        assertThat(validationService.validateData(incomingData)).isFalse();
    }

    @Test
    void validateData_unknownSchema_returnsFalse() {
        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("dataContext", "unknown");
        incomingData.put("dataContextVersion", "v0");
        incomingData.put("dataObject", Map.of("field", "value"));

        assertThat(validationService.validateData(incomingData)).isFalse();
    }
}
