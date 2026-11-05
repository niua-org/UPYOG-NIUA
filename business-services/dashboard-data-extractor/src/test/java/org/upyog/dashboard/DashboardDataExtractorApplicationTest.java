package org.upyog.dashboard;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardDataExtractorApplicationTest {
    @Test
    void classLoads() {
        assertThat(DashboardDataExtractorApplication.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardDataExtractorApplication.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
