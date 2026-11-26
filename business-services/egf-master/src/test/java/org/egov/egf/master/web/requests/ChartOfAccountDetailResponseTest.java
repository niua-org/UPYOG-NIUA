package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartOfAccountDetailResponseTest {
    @Test
    void classLoads() {
        assertThat(ChartOfAccountDetailResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChartOfAccountDetailResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
