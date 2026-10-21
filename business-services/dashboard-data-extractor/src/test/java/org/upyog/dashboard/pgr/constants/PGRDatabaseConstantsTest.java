package org.upyog.dashboard.pgr.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PGRDatabaseConstantsTest {
    @Test
    void classLoads() {
        assertThat(PGRDatabaseConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PGRDatabaseConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
