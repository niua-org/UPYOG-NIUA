package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankResponseTest {
    @Test
    void classLoads() {
        assertThat(BankResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
