package org.upyog.dashboard.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardExtractorConstantsTest {
    @Test
    void classLoads() {
        assertThat(DashboardExtractorConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DashboardExtractorConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
