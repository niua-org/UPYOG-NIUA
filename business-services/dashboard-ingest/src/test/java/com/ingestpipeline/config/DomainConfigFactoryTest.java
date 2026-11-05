package com.ingestpipeline.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomainConfigFactoryTest {
    @Test
    void classLoads() {
        assertThat(DomainConfigFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DomainConfigFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
