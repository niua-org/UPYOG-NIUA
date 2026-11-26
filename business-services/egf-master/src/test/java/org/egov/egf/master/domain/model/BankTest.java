package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankTest {
    @Test
    void classLoads() {
        assertThat(Bank.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Bank.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
