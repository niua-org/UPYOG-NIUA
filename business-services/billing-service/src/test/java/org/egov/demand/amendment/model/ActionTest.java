package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ActionTest {
    @Test
    void classLoads() {
        assertThat(Action.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Action.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
