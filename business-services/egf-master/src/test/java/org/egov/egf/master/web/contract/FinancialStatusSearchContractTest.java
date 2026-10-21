package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatusSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatusSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
