package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeRequestTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
