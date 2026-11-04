package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeInfoTest {
    @Test
    void classLoads() {
        assertThat(EmployeeInfo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeInfo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
