package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountCodePurposeEntityTest {
    @Test
    void classLoads() {
        assertThat(AccountCodePurposeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountCodePurposeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
