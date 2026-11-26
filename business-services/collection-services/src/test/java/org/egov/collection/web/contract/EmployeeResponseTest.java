package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeResponseTest {
    @Test
    void classLoads() {
        assertThat(EmployeeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
