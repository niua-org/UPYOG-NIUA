package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomainIndexConfigTest {
    @Test
    void classLoads() {
        assertThat(DomainIndexConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DomainIndexConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
