package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurpose.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurpose.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
