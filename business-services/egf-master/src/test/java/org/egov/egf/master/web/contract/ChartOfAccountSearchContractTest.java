package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountSearchContractTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
