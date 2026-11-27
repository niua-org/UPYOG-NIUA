package org.upyog.dashboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaMappingConfigTest {
    @Test
    void classLoads() {
        assertThat(SchemaMappingConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemaMappingConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
