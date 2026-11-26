package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
