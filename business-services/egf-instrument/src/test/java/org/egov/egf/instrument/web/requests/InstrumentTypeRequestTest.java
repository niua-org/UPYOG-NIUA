package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypeRequestTest {
    @Test
    void classLoads() {
        assertThat(InstrumentTypeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentTypeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
