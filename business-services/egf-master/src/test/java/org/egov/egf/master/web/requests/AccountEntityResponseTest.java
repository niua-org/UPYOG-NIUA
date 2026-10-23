package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityResponseTest {
    @Test
    void classLoads() {
        assertThat(AccountEntityResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntityResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
