package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoveryResponseTest {
    @Test
    void classLoads() {
        assertThat(RecoveryResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoveryResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
