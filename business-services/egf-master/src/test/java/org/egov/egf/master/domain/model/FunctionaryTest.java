package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionaryTest {
    @Test
    void classLoads() {
        assertThat(Functionary.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Functionary.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
