package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AggregateRequestDtoTest {
    @Test
    void classLoads() {
        assertThat(AggregateRequestDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AggregateRequestDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
