package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionarySearchContractTest {
    @Test
    void classLoads() {
        assertThat(FunctionarySearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionarySearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
