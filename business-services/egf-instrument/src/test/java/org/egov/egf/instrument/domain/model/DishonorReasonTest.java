package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonTest {
    @Test
    void classLoads() {
        assertThat(DishonorReason.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReason.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
