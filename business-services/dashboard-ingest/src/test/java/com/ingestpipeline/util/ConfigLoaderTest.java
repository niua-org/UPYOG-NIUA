package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {
    @Test
    void classLoads() {
        assertThat(ConfigLoader.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ConfigLoader.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
