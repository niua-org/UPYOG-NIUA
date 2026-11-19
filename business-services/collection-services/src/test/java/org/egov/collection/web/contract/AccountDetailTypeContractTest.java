package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeContractTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
