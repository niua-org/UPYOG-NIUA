package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SurrenderReasonEntityTest {
    @Test
    void classLoads() {
        assertThat(SurrenderReasonEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SurrenderReasonEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
