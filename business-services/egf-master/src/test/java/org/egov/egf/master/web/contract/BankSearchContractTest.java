package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankSearchContractTest {
    @Test
    void classLoads() {
        assertThat(BankSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
