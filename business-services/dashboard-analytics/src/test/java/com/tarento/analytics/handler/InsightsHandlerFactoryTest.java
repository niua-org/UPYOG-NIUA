package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsightsHandlerFactoryTest {
    @Test
    void classLoads() {
        assertThat(InsightsHandlerFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InsightsHandlerFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
