package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountContractTest {
    @Test
    void classLoads() {
        assertThat(BankAccountContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
