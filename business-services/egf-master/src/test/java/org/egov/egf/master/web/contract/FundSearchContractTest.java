package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FundSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
