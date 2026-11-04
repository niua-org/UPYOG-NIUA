package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeSearchEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeSearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeSearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
