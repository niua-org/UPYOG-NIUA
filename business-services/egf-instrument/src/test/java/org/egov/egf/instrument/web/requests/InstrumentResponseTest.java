package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentResponseTest {
    @Test
    void classLoads() {
        assertThat(InstrumentResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
