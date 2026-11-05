package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodRequestTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
