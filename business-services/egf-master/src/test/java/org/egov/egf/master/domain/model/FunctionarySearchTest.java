package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionarySearchTest {
    @Test
    void classLoads() {
        assertThat(FunctionarySearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionarySearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
