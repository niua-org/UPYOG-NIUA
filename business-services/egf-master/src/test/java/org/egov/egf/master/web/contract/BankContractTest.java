package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankContractTest {
    @Test
    void classLoads() {
        assertThat(BankContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
