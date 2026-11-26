// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloodGroupTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(BloodGroup.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        BloodGroup first = BloodGroup.values()[0];
        assertThat(BloodGroup.valueOf(first.name())).isEqualTo(first);
    }
}
