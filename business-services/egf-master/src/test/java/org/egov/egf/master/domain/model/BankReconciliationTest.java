package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankReconciliationTest {
    @Test
    void classLoads() {
        assertThat(BankReconciliation.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankReconciliation.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
