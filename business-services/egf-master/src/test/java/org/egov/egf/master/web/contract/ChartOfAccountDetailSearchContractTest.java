package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailSearchContractTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
