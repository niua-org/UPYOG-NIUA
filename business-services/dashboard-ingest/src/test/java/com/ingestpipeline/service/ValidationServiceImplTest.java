package com.ingestpipeline.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceImplTest {

    private final ValidationServiceImpl validationService = new ValidationServiceImpl();

    @Test
    void validateData_invalidPayload_returnsFalse() {
        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("dataContext", "unknown");
        incomingData.put("dataContextVersion", "v0");
        incomingData.put("dataObject", Map.of("field", "value"));

        Boolean valid = validationService.validateData(incomingData);

        assertThat(valid).isFalse();
    }
}
