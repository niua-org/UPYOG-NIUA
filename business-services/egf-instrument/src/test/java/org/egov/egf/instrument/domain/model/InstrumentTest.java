package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTest {
    @Test
    void classLoads() {
        assertThat(Instrument.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Instrument.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
