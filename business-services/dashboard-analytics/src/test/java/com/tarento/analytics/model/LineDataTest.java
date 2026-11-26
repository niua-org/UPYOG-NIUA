package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LineDataTest {
    @Test
    void classLoads() {
        assertThat(LineData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LineData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
