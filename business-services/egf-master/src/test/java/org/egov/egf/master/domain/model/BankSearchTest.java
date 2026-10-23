package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankSearchTest {
    @Test
    void classLoads() {
        assertThat(BankSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
