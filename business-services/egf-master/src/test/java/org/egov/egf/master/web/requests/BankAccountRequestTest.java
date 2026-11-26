package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountRequestTest {
    @Test
    void classLoads() {
        assertThat(BankAccountRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
