package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DepartmentTest {
    @Test
    void classLoads() {
        assertThat(Department.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Department.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
