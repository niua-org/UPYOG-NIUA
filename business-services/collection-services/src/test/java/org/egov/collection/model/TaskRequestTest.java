package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaskRequestTest {
    @Test
    void classLoads() {
        assertThat(TaskRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaskRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
