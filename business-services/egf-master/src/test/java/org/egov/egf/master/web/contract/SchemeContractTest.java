package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeContractTest {
    @Test
    void classLoads() {
        assertThat(SchemeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
