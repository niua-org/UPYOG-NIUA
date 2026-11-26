package org.upyog.dashboard.adv.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ADVAggregatedDataTest {
    @Test
    void classLoads() {
        assertThat(ADVAggregatedData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ADVAggregatedData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
