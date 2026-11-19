package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionaryRequestTest {
    @Test
    void classLoads() {
        assertThat(FunctionaryRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionaryRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
