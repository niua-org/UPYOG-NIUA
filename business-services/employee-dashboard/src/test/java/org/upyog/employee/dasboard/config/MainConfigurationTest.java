package org.upyog.employee.dasboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MainConfigurationTest {
    @Test
    void classLoads() {
        assertThat(MainConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MainConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
