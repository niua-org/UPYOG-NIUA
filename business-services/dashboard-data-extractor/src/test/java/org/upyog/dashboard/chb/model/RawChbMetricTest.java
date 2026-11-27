package org.upyog.dashboard.chb.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RawChbMetricTest {
    @Test
    void classLoads() {
        assertThat(RawChbMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RawChbMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
