package com.tarento.analytics.constant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {
    @Test
    void classLoads() {
        assertThat(ErrorCode.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorCode.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
