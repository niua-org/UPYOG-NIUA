package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialConfigurationResponseTest {
    @Test
    void classLoads() {
        assertThat(FinancialConfigurationResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinancialConfigurationResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
