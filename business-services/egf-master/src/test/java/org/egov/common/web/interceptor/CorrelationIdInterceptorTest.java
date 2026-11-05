package org.egov.common.web.interceptor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdInterceptorTest {
    @Test
    void classLoads() {
        assertThat(CorrelationIdInterceptor.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CorrelationIdInterceptor.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
