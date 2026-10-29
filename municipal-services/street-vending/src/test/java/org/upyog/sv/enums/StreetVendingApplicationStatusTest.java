// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreetVendingApplicationStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(StreetVendingApplicationStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        StreetVendingApplicationStatus first = StreetVendingApplicationStatus.values()[0];
        assertThat(StreetVendingApplicationStatus.valueOf(first.name())).isEqualTo(first);
    }
}
