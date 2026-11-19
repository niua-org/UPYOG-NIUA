// generated-by: generate_per_class_unit_tests.py
package org.egov.swcalculation.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodCycleTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(PeriodCycle.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        PeriodCycle first = PeriodCycle.values()[0];
        assertThat(PeriodCycle.valueOf(first.name())).isEqualTo(first);
    }
}
