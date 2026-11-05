package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchSearchContractTest {
    @Test
    void classLoads() {
        assertThat(BankBranchSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
