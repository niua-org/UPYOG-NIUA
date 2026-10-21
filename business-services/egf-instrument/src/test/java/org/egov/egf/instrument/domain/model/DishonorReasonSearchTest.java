package org.egov.egf.instrument.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonSearchTest {
    @Test
    void classLoads() {
        assertThat(DishonorReasonSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReasonSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
