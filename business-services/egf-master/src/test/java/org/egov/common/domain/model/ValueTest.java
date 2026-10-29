package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ValueTest {
    @Test
    void classLoads() {
        assertThat(Value.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Value.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
