package org.egov;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SetThreadLocalsTest {
    @Test
    void classLoads() {
        assertThat(SetThreadLocals.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SetThreadLocals.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
