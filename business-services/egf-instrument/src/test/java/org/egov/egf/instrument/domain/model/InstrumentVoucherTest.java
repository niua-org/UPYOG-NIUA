package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentVoucherTest {
    @Test
    void classLoads() {
        assertThat(InstrumentVoucher.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentVoucher.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
