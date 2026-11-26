package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundResponseTest {
    @Test
    void classLoads() {
        assertThat(FundResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
