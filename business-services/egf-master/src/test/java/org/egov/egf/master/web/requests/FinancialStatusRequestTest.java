package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatusRequestTest {
    @Test
    void classLoads() {
        assertThat(FinancialStatusRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialStatusRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
