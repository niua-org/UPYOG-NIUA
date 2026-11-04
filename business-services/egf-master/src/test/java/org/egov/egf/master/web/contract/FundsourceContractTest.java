package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceContractTest {
    @Test
    void classLoads() {
        assertThat(FundsourceContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
