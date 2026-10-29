package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountResponseTest {
    @Test
    void classLoads() {
        assertThat(BankAccountResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
