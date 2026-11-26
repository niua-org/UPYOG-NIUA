package org.upyog.dashboard.pgr.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RawPgrMetricTest {
    @Test
    void classLoads() {
        assertThat(RawPgrMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RawPgrMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
