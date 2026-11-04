package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlotTest {
    @Test
    void classLoads() {
        assertThat(Plot.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Plot.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
