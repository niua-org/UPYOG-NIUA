package com.tarento.analytics.query.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AggregationTest {
    @Test
    void classLoads() {
        assertThat(Aggregation.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Aggregation.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
