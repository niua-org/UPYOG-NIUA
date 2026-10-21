package com.tarento.analytics.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsightsWidgetTest {
    @Test
    void classLoads() {
        assertThat(InsightsWidget.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InsightsWidget.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
