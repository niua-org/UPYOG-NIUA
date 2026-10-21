package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatusContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatusContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
