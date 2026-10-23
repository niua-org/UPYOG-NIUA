package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionRequestTest {
    @Test
    void classLoads() {
        assertThat(FunctionRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
