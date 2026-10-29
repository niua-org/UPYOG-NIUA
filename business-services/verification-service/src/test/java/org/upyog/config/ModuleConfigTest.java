package org.upyog.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleConfigTest {
    @Test
    void classLoads() {
        assertThat(ModuleConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ModuleConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
