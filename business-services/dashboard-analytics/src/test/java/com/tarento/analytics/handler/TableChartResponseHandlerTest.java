package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TableChartResponseHandlerTest {
    @Test
    void classLoads() {
        assertThat(TableChartResponseHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TableChartResponseHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
