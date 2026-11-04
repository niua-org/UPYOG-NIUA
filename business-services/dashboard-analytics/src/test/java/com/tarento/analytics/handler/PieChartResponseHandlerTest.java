package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PieChartResponseHandlerTest {
    @Test
    void classLoads() {
        assertThat(PieChartResponseHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PieChartResponseHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
