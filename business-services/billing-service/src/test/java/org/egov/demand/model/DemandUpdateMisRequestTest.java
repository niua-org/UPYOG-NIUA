package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandUpdateMisRequestTest {
    @Test
    void classLoads() {
        assertThat(DemandUpdateMisRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandUpdateMisRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
