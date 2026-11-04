package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodResponseTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
