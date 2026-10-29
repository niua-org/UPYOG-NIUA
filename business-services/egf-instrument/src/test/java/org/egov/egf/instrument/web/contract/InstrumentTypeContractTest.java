package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
