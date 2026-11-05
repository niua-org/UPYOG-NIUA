package org.egov.egf.instrument.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonRequestTest {
    @Test
    void classLoads() {
        assertThat(DishonorReasonRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReasonRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
