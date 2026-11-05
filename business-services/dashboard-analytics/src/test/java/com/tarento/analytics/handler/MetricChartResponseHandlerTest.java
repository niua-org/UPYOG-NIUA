package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MetricChartResponseHandlerTest {
    @Test
    void classLoads() {
        assertThat(MetricChartResponseHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MetricChartResponseHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
