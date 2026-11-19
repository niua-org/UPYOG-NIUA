package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationRequestTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
