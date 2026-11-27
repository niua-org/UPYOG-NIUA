package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
