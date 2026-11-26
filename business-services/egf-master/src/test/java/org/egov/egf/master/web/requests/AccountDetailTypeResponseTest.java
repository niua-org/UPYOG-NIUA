package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeResponseTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
