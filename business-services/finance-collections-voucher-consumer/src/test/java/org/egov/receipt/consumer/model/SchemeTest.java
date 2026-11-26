package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeTest {
    @Test
    void classLoads() {
        assertThat(Scheme.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Scheme.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
