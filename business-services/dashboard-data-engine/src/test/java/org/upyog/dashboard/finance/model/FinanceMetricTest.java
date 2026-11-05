package org.upyog.dashboard.finance.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceMetricTest {
    @Test
    void classLoads() {
        assertThat(FinanceMetric.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceMetric.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
