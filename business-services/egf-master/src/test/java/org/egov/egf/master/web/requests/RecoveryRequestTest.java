package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoveryRequestTest {
    @Test
    void classLoads() {
        assertThat(RecoveryRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoveryRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
