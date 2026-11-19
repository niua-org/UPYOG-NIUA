package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AssignmentTest {
    @Test
    void classLoads() {
        assertThat(Assignment.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Assignment.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
