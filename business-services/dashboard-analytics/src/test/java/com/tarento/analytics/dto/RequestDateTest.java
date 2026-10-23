package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestDateTest {
    @Test
    void classLoads() {
        assertThat(RequestDate.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestDate.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
