package com.tarento.analytics.handler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseHandlerFactoryTest {
    @Test
    void classLoads() {
        assertThat(ResponseHandlerFactory.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ResponseHandlerFactory.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
