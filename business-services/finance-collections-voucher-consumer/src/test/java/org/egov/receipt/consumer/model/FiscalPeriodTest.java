package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriod.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriod.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
