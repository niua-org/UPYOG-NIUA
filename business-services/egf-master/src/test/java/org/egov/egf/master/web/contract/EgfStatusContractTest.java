package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgfStatusContractTest {
    @Test
    void classLoads() {
        assertThat(EgfStatusContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgfStatusContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
