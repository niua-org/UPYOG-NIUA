// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.models.demand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(DemandStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        DemandStatus first = DemandStatus.values()[0];
        assertThat(DemandStatus.valueOf(first.name())).isEqualTo(first);
    }
}
