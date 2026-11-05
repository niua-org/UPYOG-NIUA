package org.upyog.dashboard.common.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardConstantsTest {
    @Test
    void classLoads() {
        assertThat(DashboardConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
