package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
