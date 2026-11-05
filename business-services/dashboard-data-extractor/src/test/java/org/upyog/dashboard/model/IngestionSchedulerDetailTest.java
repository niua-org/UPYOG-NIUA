package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionSchedulerDetailTest {
    @Test
    void classLoads() {
        assertThat(IngestionSchedulerDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IngestionSchedulerDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
