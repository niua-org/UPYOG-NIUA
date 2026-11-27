package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchSearchTest {
    @Test
    void classLoads() {
        assertThat(BankBranchSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
