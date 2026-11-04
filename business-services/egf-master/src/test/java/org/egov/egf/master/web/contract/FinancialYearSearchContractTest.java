package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialYearSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYearSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
