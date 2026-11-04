package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialYearRequestTest {
    @Test
    void classLoads() {
        assertThat(FinancialYearRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialYearRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
