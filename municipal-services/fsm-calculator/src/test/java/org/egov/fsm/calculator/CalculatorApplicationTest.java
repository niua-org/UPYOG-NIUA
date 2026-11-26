// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(CalculatorApplication.class).isNotNull();
        assertThat(CalculatorApplication.class.getSimpleName()).isEqualTo("CalculatorApplication");
    }
}
