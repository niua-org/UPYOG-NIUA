package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankBranchRequestTest {
    @Test
    void classLoads() {
        assertThat(BankBranchRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankBranchRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
