// generated-by: generate_per_class_unit_tests.py
package upyog.notification.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(NotificationChannel.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        NotificationChannel first = NotificationChannel.values()[0];
        assertThat(NotificationChannel.valueOf(first.name())).isEqualTo(first);
    }
}
