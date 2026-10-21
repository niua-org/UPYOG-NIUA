package org.egov.bpa.calculator.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationUtilsTest {

    @InjectMocks
    private CalculationUtils calculationUtils;

    @Test
    @DisplayName("calculationUtils_whenInjected_isNotNull")
    void calculationUtils_whenInjected_isNotNull() {
        assertThat(calculationUtils).isNotNull();
    }
}
