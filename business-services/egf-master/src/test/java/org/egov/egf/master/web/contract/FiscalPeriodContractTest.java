package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FiscalPeriodContractTest {
    @Test
    void classLoads() {
        assertThat(FiscalPeriodContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FiscalPeriodContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
