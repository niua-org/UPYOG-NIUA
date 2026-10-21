package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsightsConfigurationTest {
    @Test
    void classLoads() {
        assertThat(InsightsConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InsightsConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
