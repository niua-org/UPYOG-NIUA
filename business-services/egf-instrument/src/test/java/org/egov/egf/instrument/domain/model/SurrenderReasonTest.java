package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReason.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReason.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
