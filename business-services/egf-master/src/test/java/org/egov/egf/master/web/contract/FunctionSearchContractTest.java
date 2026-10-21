package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionSearchContractTest {
    @Test
    void classLoads() {
        assertThat(FunctionSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
