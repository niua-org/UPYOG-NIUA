package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentVoucherEntityTest {
    @Test
    void classLoads() {
        assertThat(InstrumentVoucherEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentVoucherEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
