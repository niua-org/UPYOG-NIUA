// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChallanStatusEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(ChallanStatusEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        ChallanStatusEnum first = ChallanStatusEnum.values()[0];
        assertThat(ChallanStatusEnum.valueOf(first.name())).isEqualTo(first);
    }
}
