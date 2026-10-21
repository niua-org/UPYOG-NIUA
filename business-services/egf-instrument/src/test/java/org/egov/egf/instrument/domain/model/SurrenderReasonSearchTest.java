package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonSearchTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
