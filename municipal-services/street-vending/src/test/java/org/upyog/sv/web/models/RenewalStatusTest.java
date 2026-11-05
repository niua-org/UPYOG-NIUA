// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenewalStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(RenewalStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        RenewalStatus first = RenewalStatus.values()[0];
        assertThat(RenewalStatus.valueOf(first.name())).isEqualTo(first);
    }
}
