package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearSearchTest {
    @Test
    void classLoads() {
        assertThat(FinancialYearSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYearSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
