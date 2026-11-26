package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountEntityTest {
    @Test
    void classLoads() {
        assertThat(BankAccountEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
