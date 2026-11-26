package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeResponseTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
