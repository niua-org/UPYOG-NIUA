// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestServiceStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(RequestServiceStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        RequestServiceStatus first = RequestServiceStatus.values()[0];
        assertThat(RequestServiceStatus.valueOf(first.name())).isEqualTo(first);
    }
}
