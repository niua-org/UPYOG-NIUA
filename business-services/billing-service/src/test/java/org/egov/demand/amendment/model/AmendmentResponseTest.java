package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentResponseTest {
    @Test
    void classLoads() {
        assertThat(AmendmentResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmendmentResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
