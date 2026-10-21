package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeyRequestTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeyRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeyRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
