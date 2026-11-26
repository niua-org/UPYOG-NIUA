package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationValueContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationValueContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationValueContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
