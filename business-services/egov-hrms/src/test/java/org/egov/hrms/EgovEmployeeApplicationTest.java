package org.egov.hrms;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgovEmployeeApplicationTest {
    @Test
    void classLoads() {
        assertThat(EgovEmployeeApplication.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgovEmployeeApplication.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
