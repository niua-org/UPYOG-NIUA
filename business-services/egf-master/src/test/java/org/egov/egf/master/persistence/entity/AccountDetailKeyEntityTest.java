package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeyEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeyEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeyEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
