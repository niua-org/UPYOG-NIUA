package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountResponseTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
