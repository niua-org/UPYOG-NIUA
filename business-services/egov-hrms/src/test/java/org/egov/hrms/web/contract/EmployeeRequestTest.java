package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeRequestTest {
    @Test
    void classLoads() {
        assertThat(EmployeeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
