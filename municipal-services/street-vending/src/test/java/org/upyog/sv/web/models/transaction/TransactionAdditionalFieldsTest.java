// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.web.models.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionAdditionalFieldsTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(TransactionAdditionalFields.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        TransactionAdditionalFields first = TransactionAdditionalFields.values()[0];
        assertThat(TransactionAdditionalFields.valueOf(first.name())).isEqualTo(first);
    }
}
