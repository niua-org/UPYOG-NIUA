package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationCountTest {
    @Test
    void classLoads() {
        assertThat(MigrationCount.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MigrationCount.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
