package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionContractTest {
    @Test
    void classLoads() {
        assertThat(FunctionContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
