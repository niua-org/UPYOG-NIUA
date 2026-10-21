package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxPeriodResponseTest {
    @Test
    void classLoads() {
        assertThat(TaxPeriodResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxPeriodResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
