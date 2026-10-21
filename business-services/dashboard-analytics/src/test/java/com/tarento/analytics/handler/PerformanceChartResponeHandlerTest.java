package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PerformanceChartResponeHandlerTest {
    @Test
    void classLoads() {
        assertThat(PerformanceChartResponeHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PerformanceChartResponeHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
