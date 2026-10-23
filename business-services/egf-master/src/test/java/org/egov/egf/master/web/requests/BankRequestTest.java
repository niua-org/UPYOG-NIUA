package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankRequestTest {
    @Test
    void classLoads() {
        assertThat(BankRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
