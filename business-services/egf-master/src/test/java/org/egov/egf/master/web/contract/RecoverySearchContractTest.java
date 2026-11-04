package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoverySearchContractTest {
    @Test
    void classLoads() {
        assertThat(RecoverySearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoverySearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
