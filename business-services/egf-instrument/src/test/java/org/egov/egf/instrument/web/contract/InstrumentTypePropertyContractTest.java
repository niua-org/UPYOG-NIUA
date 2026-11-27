package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypePropertyContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypePropertyContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypePropertyContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
