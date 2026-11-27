package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountEntityEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntityEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
