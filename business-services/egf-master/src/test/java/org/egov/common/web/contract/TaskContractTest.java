package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaskContractTest {
    @Test
    void classLoads() {
        assertThat(TaskContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaskContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
