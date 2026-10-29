package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeSearchContractTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
