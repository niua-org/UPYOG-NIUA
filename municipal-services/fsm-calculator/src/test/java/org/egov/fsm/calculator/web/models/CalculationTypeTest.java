// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(CalculationType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        CalculationType first = CalculationType.values()[0];
        assertThat(CalculationType.valueOf(first.name())).isEqualTo(first);
    }
}
