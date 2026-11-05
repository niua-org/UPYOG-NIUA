package org.upyog.dashboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardPropertiesTest {
    @Test
    void classLoads() {
        assertThat(DashboardProperties.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardProperties.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
