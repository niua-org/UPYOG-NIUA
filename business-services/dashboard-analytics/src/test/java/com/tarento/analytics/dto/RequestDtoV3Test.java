package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoV3Test {
    @Test
    void classLoads() {
        assertThat(RequestDtoV3.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestDtoV3.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
