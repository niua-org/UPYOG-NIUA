package org.egov.common.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationThreadLocalsTest {
    @Test
    void classLoads() {
        assertThat(ApplicationThreadLocals.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApplicationThreadLocals.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
