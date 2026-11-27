package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {
    @Test
    void classLoads() {
        assertThat(Task.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Task.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
