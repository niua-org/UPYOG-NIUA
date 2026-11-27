package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialYearContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYearContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
