package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailRequestTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
