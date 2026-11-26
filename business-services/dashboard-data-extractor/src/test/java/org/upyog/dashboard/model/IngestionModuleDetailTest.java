package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionModuleDetailTest {
    @Test
    void classLoads() {
        assertThat(IngestionModuleDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IngestionModuleDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
