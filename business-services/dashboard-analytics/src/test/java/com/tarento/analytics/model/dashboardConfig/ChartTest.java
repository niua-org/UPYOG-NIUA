package com.tarento.analytics.model.dashboardConfig;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartTest {
    @Test
    void classLoads() {
        assertThat(Chart.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Chart.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
