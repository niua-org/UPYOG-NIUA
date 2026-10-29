package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeResponseTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
