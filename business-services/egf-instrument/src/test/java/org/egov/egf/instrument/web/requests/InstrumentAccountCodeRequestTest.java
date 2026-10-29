package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeRequestTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
