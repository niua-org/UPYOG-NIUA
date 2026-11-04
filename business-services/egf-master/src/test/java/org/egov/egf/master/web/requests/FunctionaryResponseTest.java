package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionaryResponseTest {
    @Test
    void classLoads() {
        assertThat(FunctionaryResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionaryResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
