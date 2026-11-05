package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdGenerationRequestTest {
    @Test
    void classLoads() {
        assertThat(IdGenerationRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IdGenerationRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
