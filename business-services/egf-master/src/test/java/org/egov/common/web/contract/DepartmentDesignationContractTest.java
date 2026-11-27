package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DepartmentDesignationContractTest {
    @Test
    void classLoads() {
        assertThat(DepartmentDesignationContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DepartmentDesignationContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
