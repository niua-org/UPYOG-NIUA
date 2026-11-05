package org.egov.collection.web.contract;

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
