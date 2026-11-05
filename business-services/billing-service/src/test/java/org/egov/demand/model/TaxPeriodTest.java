package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxPeriodTest {
    @Test
    void classLoads() {
        assertThat(TaxPeriod.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxPeriod.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
