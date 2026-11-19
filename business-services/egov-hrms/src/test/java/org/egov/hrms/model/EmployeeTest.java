package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeTest {
    @Test
    void classLoads() {
        assertThat(Employee.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Employee.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
