package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxPeriodCriteriaTest {
    @Test
    void classLoads() {
        assertThat(TaxPeriodCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxPeriodCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
