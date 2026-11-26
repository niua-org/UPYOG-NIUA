package com.tarento.analytics.query.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class YAxisTest {
    @Test
    void classLoads() {
        assertThat(YAxis.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = YAxis.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
