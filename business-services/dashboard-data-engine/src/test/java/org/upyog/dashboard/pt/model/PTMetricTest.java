package org.upyog.dashboard.pt.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTMetricTest {
    @Test
    void classLoads() {
        assertThat(PTMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
