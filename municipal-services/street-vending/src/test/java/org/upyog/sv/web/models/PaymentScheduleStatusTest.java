// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentScheduleStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(PaymentScheduleStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        PaymentScheduleStatus first = PaymentScheduleStatus.values()[0];
        assertThat(PaymentScheduleStatus.valueOf(first.name())).isEqualTo(first);
    }
}
