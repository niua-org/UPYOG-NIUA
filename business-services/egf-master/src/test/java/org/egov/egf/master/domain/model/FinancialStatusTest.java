package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatus.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatus.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
