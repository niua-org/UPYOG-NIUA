package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountTest {
    @Test
    void classLoads() {
        assertThat(BankAccount.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccount.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
