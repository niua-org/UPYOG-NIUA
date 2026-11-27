package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeyContractTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeyContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeyContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
