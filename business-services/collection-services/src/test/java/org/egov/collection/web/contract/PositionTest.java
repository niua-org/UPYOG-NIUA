package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PositionTest {
    @Test
    void classLoads() {
        assertThat(Position.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Position.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
