package org.upyog.employee.dasboard.kafka;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class ConsumerTest {
    @ExtendWith(MockitoExtension.class)
    class UnitTests {
        @Test
        void classLoads() {
            assertThat(Consumer.class).isNotNull();
        }
    }
}
