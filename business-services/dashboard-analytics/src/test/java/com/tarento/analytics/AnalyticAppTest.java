package com.tarento.analytics;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AnalyticAppTest {
    @Test
    void classLoads() {
        assertThat(AnalyticApp.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AnalyticApp.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
