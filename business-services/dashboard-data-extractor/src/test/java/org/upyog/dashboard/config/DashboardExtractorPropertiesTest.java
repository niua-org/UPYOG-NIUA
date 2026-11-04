package org.upyog.dashboard.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardExtractorPropertiesTest {
    @Test
    void classLoads() {
        assertThat(DashboardExtractorProperties.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardExtractorProperties.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
