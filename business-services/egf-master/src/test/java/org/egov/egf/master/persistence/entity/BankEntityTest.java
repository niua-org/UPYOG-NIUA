package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankEntityTest {
    @Test
    void classLoads() {
        assertThat(BankEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
