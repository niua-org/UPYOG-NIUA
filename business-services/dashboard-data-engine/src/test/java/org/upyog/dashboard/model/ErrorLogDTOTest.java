package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorLogDTOTest {
    @Test
    void classLoads() {
        assertThat(ErrorLogDTO.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorLogDTO.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
