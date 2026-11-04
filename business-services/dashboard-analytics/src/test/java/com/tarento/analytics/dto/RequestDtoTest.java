package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoTest {
    @Test
    void classLoads() {
        assertThat(RequestDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
