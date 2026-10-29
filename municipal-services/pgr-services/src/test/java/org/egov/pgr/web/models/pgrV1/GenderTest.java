// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.web.models.pgrV1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenderTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Gender.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Gender first = Gender.values()[0];
        assertThat(Gender.valueOf(first.name())).isEqualTo(first);
    }
}
