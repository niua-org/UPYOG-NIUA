package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntitySearchEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountEntitySearchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntitySearchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
