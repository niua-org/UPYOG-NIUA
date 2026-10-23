package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountSearchContractTest {
    @Test
    void classLoads() {
        assertThat(BankAccountSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
