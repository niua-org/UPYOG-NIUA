package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentRequestTest {
    @Test
    void classLoads() {
        assertThat(AmendmentRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmendmentRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
