package org.upyog.reconciliation.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReconConfigurationTest {
    @Test
    void classLoads() {
        assertThat(ReconConfiguration.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReconConfiguration.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
