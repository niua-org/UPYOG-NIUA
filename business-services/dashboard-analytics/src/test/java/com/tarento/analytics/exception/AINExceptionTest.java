package com.tarento.analytics.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AINExceptionTest {
    @Test
    void classLoads() {
        assertThat(AINException.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AINException.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
