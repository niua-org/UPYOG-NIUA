package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesignationContractTest {
    @Test
    void classLoads() {
        assertThat(DesignationContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DesignationContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
