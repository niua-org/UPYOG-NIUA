package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AggregateRequestDtoV3Test {
    @Test
    void classLoads() {
        assertThat(AggregateRequestDtoV3.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AggregateRequestDtoV3.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
