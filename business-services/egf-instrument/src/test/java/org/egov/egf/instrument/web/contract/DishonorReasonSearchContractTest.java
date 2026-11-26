package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DishonorReasonSearchContractTest {
    @Test
    void classLoads() {
        assertThat(DishonorReasonSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DishonorReasonSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
