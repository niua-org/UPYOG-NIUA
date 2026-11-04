package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentVoucherContractTest {
    @Test
    void classLoads() {
        assertThat(InstrumentVoucherContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentVoucherContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
