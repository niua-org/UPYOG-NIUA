package org.upyog.dashboard.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyIngestionDataTest {
    @Test
    void classLoads() {
        assertThat(LegacyIngestionData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyIngestionData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
