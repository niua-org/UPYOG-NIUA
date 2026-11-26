package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeySearchEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeySearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeySearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
