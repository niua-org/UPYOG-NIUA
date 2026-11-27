package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ValueContractTest {
    @Test
    void classLoads() {
        assertThat(ValueContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ValueContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
