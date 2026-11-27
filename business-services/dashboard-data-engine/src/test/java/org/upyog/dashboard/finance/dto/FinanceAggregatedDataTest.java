package org.upyog.dashboard.finance.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceAggregatedDataTest {
    @Test
    void classLoads() {
        assertThat(FinanceAggregatedData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceAggregatedData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
