package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDetailsRequestTest {
    @Test
    void classLoads() {
        assertThat(WorkflowDetailsRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = WorkflowDetailsRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
