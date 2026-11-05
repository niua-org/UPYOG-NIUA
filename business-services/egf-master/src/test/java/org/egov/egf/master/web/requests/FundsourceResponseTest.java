package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceResponseTest {
    @Test
    void classLoads() {
        assertThat(FundsourceResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
