package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusSearchTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatusSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatusSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
