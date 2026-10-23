package org.egov.egf.instrument.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonEntityTest {
    @Test
    void classLoads() {
        assertThat(DishonorReasonEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReasonEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
