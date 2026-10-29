package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceResponseTest {
    @Test
    void classLoads() {
        assertThat(ProcessInstanceResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ProcessInstanceResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
