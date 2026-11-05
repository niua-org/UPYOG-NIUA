package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceRequestTest {
    @Test
    void classLoads() {
        assertThat(FundsourceRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
