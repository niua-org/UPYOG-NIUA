// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentModeEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(PaymentModeEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        PaymentModeEnum first = PaymentModeEnum.values()[0];
        assertThat(PaymentModeEnum.valueOf(first.name())).isEqualTo(first);
    }
}
