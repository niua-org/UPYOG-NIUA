package org.upyog.dashboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {
    @Test
    void classLoads() {
        assertThat(AppConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AppConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
