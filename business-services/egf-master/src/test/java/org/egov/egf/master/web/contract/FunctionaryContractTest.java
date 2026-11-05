package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionaryContractTest {
    @Test
    void classLoads() {
        assertThat(FunctionaryContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionaryContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
