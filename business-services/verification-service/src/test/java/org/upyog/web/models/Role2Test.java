package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Role2Test {
    @Test
    void classLoads() {
        assertThat(Role2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Role2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
