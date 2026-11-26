package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountRequestTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
