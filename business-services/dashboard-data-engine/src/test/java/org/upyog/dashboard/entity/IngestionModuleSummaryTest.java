package org.upyog.dashboard.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionModuleSummaryTest {
    @Test
    void classLoads() {
        assertThat(IngestionModuleSummary.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IngestionModuleSummary.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
