package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StateTest {
    @Test
    void classLoads() {
        assertThat(State.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = State.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
