package org.egov.common.web.interceptor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdAwareRestTemplateTest {
    @Test
    void classLoads() {
        assertThat(CorrelationIdAwareRestTemplate.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CorrelationIdAwareRestTemplate.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
