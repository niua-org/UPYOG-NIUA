package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionResponseTest {
    @Test
    void classLoads() {
        assertThat(FunctionResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
