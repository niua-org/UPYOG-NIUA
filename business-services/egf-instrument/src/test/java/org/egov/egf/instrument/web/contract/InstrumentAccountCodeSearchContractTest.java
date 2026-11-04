package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeSearchContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
