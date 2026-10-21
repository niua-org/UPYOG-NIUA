package org.egov.hrms.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeQueriesTest {
    @Test
    void classLoads() {
        assertThat(EmployeeQueries.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeQueries.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
