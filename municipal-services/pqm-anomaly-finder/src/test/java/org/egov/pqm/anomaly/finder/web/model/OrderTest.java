// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.anomaly.finder.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Order.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Order first = Order.values()[0];
        assertThat(Order.valueOf(first.name())).isEqualTo(first);
    }
}
