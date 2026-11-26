package com.ingestpipeline.configfactory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EnhanceDomainConfigTest {
    @Test
    void classLoads() {
        assertThat(EnhanceDomainConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EnhanceDomainConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
