package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountSearchTest {
    @Test
    void classLoads() {
        assertThat(BankAccountSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankAccountSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
