package org.upyog.dashboard.pt.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTDatabaseConstantsTest {
    @Test
    void classLoads() {
        assertThat(PTDatabaseConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTDatabaseConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
