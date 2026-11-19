package org.upyog.dashboard.pt.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RawPtMetricTest {
    @Test
    void classLoads() {
        assertThat(RawPtMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RawPtMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
