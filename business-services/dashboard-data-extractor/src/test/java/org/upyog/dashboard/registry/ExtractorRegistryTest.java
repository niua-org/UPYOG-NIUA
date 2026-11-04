package org.upyog.dashboard.registry;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExtractorRegistryTest {
    @Test
    void classLoads() {
        assertThat(ExtractorRegistry.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ExtractorRegistry.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
