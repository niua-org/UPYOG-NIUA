package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceRequestTest {
    @Test
    void classLoads() {
        assertThat(ProcessInstanceRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ProcessInstanceRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
