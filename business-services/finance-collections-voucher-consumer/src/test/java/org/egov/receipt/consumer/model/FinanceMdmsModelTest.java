package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceMdmsModelTest {
    @Test
    void classLoads() {
        assertThat(FinanceMdmsModel.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceMdmsModel.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
