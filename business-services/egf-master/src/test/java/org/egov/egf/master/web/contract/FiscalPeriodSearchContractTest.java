package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
