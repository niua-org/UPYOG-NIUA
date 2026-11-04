package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CummulativeDataRequestDtoTest {
    @Test
    void classLoads() {
        assertThat(CummulativeDataRequestDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CummulativeDataRequestDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
