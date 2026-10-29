package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeSearchContractTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
