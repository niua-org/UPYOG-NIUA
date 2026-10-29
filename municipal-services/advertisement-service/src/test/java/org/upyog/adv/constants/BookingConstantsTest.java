// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConstantsTest {

    @Test
    void BOOKING_CREATED_whenRead_isNotBlank() {
        assertThat(BookingConstants.BOOKING_CREATED).isNotBlank();
    }

    @Test
    void constructor_whenPrivate_rejectsUtilityInstantiation() throws Exception {
        var ctor = BookingConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(ctor::newInstance)
                        .hasCauseInstanceOf(IllegalStateException.class);
    }

}
