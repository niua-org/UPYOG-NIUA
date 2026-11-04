package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeContractTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
