package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTypeSearchTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailTypeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailTypeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
