package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorResTest {
    @Test
    void classLoads() {
        assertThat(ErrorRes.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorRes.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
