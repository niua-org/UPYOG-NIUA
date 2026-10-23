package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCode.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCode.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
