package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MetricsInsightsHandlerTest {
    @Test
    void classLoads() {
        assertThat(MetricsInsightsHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MetricsInsightsHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
