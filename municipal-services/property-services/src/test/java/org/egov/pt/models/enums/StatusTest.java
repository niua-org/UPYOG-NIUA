// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Status.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Status first = Status.values()[0];
        assertThat(Status.valueOf(first.name())).isEqualTo(first);
    }
}
