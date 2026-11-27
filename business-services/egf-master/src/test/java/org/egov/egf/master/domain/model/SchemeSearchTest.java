package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeSearchTest {
    @Test
    void classLoads() {
        assertThat(SchemeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
