// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Priority.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Priority first = Priority.values()[0];
        assertThat(Priority.valueOf(first.name())).isEqualTo(first);
    }
}
