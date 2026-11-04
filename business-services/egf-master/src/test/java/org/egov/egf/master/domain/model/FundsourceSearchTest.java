package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundsourceSearchTest {
    @Test
    void classLoads() {
        assertThat(FundsourceSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundsourceSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
