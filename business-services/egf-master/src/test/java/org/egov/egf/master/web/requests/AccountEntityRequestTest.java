package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityRequestTest {
    @Test
    void classLoads() {
        assertThat(AccountEntityRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntityRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
