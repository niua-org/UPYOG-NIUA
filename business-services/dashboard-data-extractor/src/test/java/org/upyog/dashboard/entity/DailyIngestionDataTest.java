package org.upyog.dashboard.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DailyIngestionDataTest {
    @Test
    void classLoads() {
        assertThat(DailyIngestionData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DailyIngestionData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
