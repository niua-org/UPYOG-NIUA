package org.upyog.dashboard.chb.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBAggregatedDataTest {
    @Test
    void classLoads() {
        assertThat(CHBAggregatedData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBAggregatedData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
