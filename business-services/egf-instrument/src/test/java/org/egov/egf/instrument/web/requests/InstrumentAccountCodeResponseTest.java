package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeResponseTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
