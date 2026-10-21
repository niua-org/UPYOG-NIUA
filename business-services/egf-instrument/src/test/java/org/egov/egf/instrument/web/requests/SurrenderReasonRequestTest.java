package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonRequestTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
