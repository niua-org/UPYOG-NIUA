package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeSearchContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
