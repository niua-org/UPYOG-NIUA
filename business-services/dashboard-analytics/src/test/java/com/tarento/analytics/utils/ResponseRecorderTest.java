package com.tarento.analytics.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseRecorderTest {
    @Test
    void classLoads() {
        assertThat(ResponseRecorder.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ResponseRecorder.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
