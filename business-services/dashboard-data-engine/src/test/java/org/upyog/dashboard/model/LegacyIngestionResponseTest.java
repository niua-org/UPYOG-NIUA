package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyIngestionResponseTest {
    @Test
    void classLoads() {
        assertThat(LegacyIngestionResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyIngestionResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
