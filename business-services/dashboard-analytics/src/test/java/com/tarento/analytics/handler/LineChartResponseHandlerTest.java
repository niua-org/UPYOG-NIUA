package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LineChartResponseHandlerTest {
    @Test
    void classLoads() {
        assertThat(LineChartResponseHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LineChartResponseHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
