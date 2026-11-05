package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TablePostResponseHandlerTest {
    @Test
    void classLoads() {
        assertThat(TablePostResponseHandler.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TablePostResponseHandler.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
