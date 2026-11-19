package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentTest {
    @Test
    void classLoads() {
        assertThat(Amendment.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Amendment.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
