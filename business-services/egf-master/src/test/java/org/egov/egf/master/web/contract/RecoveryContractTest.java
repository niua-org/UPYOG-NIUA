package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoveryContractTest {
    @Test
    void classLoads() {
        assertThat(RecoveryContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoveryContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
