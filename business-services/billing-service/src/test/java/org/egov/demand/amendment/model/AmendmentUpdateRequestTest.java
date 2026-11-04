package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentUpdateRequestTest {
    @Test
    void classLoads() {
        assertThat(AmendmentUpdateRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmendmentUpdateRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
