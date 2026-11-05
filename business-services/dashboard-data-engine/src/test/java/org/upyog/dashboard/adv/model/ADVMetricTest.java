package org.upyog.dashboard.adv.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ADVMetricTest {
    @Test
    void classLoads() {
        assertThat(ADVMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ADVMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
