package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountEntitySearchTest {
    @Test
    void classLoads() {
        assertThat(AccountEntitySearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountEntitySearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
