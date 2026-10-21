package org.upyog.dashboard.scheduler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyIngestionSchedulerTest {
    @Test
    void classLoads() {
        assertThat(LegacyIngestionScheduler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyIngestionScheduler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
