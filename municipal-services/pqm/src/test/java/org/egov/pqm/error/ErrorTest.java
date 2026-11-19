// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Error.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Error first = Error.values()[0];
        assertThat(Error.valueOf(first.name())).isEqualTo(first);
    }
}
