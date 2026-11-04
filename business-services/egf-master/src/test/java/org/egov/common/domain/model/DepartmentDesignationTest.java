package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DepartmentDesignationTest {
    @Test
    void classLoads() {
        assertThat(DepartmentDesignation.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DepartmentDesignation.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
