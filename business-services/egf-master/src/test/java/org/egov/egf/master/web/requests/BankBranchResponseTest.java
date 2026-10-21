package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchResponseTest {
    @Test
    void classLoads() {
        assertThat(BankBranchResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
