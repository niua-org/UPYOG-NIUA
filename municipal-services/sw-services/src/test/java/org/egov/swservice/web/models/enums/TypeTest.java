// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.web.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Type.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Type first = Type.values()[0];
        assertThat(Type.valueOf(first.name())).isEqualTo(first);
    }
}
