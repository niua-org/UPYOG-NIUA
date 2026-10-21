package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntitySearchContractTest {
    @Test
    void classLoads() {
        assertThat(AccountEntitySearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntitySearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
