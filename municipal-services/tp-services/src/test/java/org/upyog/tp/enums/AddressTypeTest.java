// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(AddressType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        AddressType first = AddressType.values()[0];
        assertThat(AddressType.valueOf(first.name())).isEqualTo(first);
    }
}
