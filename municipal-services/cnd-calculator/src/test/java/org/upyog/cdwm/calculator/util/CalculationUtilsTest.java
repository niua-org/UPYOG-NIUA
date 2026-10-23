package org.upyog.cdwm.calculator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationUtilsTest {

    @Test
    @DisplayName("getCurrentTimestamp_whenCalled_returnsPositiveValue")
    void getCurrentTimestamp_whenCalled_returnsPositiveValue() {
        assertThat(CalculationUtils.getCurrentTimestamp()).isPositive();
    }
}
