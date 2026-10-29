package org.upyog.dashboard.chb.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBDatabaseConstantsTest {
    @Test
    void classLoads() {
        assertThat(CHBDatabaseConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBDatabaseConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
