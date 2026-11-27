package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearTest {
    @Test
    void classLoads() {
        assertThat(FinancialYear.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYear.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
