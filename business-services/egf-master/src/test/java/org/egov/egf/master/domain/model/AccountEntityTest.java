package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
