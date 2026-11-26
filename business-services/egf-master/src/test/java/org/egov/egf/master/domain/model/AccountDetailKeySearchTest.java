package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeySearchTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKeySearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKeySearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
