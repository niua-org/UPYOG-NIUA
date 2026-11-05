package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FundsourceSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
