package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonResponseTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
