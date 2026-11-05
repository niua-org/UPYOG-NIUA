// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.web.model.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurposeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Purpose.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Purpose first = Purpose.values()[0];
        assertThat(Purpose.valueOf(first.name())).isEqualTo(first);
    }
}
