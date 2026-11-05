// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStatusEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(BookingStatusEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        BookingStatusEnum first = BookingStatusEnum.values()[0];
        assertThat(BookingStatusEnum.valueOf(first.name())).isEqualTo(first);
    }
}
