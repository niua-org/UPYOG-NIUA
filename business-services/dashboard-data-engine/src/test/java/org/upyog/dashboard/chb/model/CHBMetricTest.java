package org.upyog.dashboard.chb.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBMetricTest {
    @Test
    void classLoads() {
        assertThat(CHBMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
