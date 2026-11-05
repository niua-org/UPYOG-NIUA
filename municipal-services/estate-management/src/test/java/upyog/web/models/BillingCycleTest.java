// generated-by: generate_per_class_unit_tests.py
package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCycleTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(BillingCycle.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        BillingCycle first = BillingCycle.values()[0];
        assertThat(BillingCycle.valueOf(first.name())).isEqualTo(first);
    }
}
