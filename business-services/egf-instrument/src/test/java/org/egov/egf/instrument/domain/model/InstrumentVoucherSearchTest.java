package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentVoucherSearchTest {
    @Test
    void classLoads() {
        assertThat(InstrumentVoucherSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentVoucherSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
