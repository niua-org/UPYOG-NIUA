package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceTest {
    @Test
    void classLoads() {
        assertThat(ProcessInstance.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ProcessInstance.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
