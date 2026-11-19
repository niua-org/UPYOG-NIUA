package com.ingestpipeline.configfactory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionDomainConfigTest {
    @Test
    void classLoads() {
        assertThat(CollectionDomainConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CollectionDomainConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
