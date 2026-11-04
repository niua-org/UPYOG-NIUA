package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchContractTest {
    @Test
    void classLoads() {
        assertThat(BankBranchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
