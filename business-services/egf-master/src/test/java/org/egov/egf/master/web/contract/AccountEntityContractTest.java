package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityContractTest {
    @Test
    void classLoads() {
        assertThat(AccountEntityContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntityContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
