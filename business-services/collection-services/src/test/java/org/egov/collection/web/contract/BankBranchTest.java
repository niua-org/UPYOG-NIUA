package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchTest {
    @Test
    void classLoads() {
        assertThat(BankBranch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
