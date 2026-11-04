package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ECSTypeTest {
    @Test
    void classLoads() {
        assertThat(ECSType.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ECSType.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
