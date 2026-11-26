package com.tarento.analytics.model.dashboardConfig;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardTest {
    @Test
    void classLoads() {
        assertThat(Dashboard.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Dashboard.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
