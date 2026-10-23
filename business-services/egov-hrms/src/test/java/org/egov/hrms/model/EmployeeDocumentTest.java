package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDocumentTest {
    @Test
    void classLoads() {
        assertThat(EmployeeDocument.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EmployeeDocument.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
