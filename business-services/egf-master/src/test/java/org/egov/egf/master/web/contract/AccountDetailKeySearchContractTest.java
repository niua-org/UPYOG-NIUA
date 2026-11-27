package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeySearchContractTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeySearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeySearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
