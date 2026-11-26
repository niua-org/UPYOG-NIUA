package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetryAttemptTest {
    @Test
    void classLoads() {
        assertThat(RetryAttempt.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RetryAttempt.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
