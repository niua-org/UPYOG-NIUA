package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonContractTest {
    @Test
    void classLoads() {
        assertThat(DishonorReasonContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReasonContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
