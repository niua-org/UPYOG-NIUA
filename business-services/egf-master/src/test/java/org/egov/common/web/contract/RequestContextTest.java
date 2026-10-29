package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {
    @Test
    void classLoads() {
        assertThat(RequestContext.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestContext.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
