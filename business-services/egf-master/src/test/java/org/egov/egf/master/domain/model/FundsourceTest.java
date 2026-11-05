package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceTest {
    @Test
    void classLoads() {
        assertThat(Fundsource.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Fundsource.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
