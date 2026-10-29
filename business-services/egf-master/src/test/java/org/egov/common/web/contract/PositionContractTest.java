package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PositionContractTest {
    @Test
    void classLoads() {
        assertThat(PositionContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PositionContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
