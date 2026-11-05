package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonSearchContractTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
