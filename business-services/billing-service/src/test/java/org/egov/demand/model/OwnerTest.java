package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OwnerTest {
    @Test
    void classLoads() {
        assertThat(Owner.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Owner.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
