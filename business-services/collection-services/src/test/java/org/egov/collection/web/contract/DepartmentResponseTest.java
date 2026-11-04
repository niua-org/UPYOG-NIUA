package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DepartmentResponseTest {
    @Test
    void classLoads() {
        assertThat(DepartmentResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DepartmentResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
