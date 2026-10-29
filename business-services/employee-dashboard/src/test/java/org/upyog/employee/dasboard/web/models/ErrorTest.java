package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorTest {
    @Test
    void classLoads() {
        assertThat(Error.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Error.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
