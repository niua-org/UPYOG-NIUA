package com.tarento.analytics;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationLoaderTest {
    @Test
    void classLoads() {
        assertThat(ConfigurationLoader.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ConfigurationLoader.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
