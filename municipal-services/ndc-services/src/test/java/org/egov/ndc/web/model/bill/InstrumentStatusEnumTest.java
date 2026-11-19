// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstrumentStatusEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(InstrumentStatusEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        InstrumentStatusEnum first = InstrumentStatusEnum.values()[0];
        assertThat(InstrumentStatusEnum.valueOf(first.name())).isEqualTo(first);
    }
}
