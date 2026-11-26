// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCreatedByEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(ApplicationCreatedByEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        ApplicationCreatedByEnum first = ApplicationCreatedByEnum.values()[0];
        assertThat(ApplicationCreatedByEnum.valueOf(first.name())).isEqualTo(first);
    }
}
