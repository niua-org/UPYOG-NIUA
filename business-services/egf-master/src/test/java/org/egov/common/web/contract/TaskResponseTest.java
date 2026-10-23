package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaskResponseTest {
    @Test
    void classLoads() {
        assertThat(TaskResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaskResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
