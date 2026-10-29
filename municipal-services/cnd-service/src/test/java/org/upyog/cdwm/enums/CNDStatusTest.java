// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CNDStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(CNDStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        CNDStatus first = CNDStatus.values()[0];
        assertThat(CNDStatus.valueOf(first.name())).isEqualTo(first);
    }
}
