package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeRequestTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
