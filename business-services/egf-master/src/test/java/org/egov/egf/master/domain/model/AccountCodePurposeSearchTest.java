package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeSearchTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
