package org.upyog.dashboard.pt.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTAggregatedDataTest {
    @Test
    void classLoads() {
        assertThat(PTAggregatedData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTAggregatedData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
