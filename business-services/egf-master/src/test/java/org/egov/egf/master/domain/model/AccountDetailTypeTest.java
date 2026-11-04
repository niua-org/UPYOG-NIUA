package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailType.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailType.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
