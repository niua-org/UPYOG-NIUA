// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(PaymentStatusEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        PaymentStatusEnum first = PaymentStatusEnum.values()[0];
        assertThat(PaymentStatusEnum.valueOf(first.name())).isEqualTo(first);
    }
}
