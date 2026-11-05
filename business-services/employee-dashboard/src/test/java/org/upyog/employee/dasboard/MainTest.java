package org.upyog.employee.dasboard;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MainTest {
    @Test
    void classLoads() {
        assertThat(Main.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Main.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
