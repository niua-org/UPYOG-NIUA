package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchEntityTest {
    @Test
    void classLoads() {
        assertThat(BankBranchEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
