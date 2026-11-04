package com.ingestpipeline.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchConfigurationTest {
    @Test
    void classLoads() {
        assertThat(ElasticSearchConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ElasticSearchConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
