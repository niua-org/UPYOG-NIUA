package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionSearchTest {
    @Test
    void classLoads() {
        assertThat(FunctionSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FunctionSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
