// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreationReasonTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(CreationReason.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        CreationReason first = CreationReason.values()[0];
        assertThat(CreationReason.valueOf(first.name())).isEqualTo(first);
    }
}
