package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AggregateDtoTest {
    @Test
    void classLoads() {
        assertThat(AggregateDto.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AggregateDto.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
