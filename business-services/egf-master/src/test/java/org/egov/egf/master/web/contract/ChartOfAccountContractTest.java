package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountContractTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
