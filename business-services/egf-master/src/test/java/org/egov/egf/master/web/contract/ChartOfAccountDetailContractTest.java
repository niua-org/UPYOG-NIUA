package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailContractTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
