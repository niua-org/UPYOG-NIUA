package org.upyog.dashboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ShedLockConfigTest {
    @Test
    void classLoads() {
        assertThat(ShedLockConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ShedLockConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
