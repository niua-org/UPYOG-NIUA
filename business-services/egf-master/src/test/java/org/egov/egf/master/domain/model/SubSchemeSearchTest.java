package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeSearchTest {
    @Test
    void classLoads() {
        assertThat(SubSchemeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubSchemeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
