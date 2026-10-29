package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemeSearchContractTest {
    @Test
    void classLoads() {
        assertThat(SchemeSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SchemeSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
