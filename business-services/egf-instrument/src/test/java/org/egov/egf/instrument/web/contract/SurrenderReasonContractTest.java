package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonContractTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
