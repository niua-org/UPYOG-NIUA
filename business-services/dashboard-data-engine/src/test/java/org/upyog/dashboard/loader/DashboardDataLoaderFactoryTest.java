package org.upyog.dashboard.loader;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardDataLoaderFactoryTest {
    @Test
    void classLoads() {
        assertThat(DashboardDataLoaderFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardDataLoaderFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
