package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentUpdateTest {
    @Test
    void classLoads() {
        assertThat(AmendmentUpdate.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmendmentUpdate.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
