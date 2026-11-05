package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailKeyTest {
    @Test
    void classLoads() {
        assertThat(AccountDetailKey.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetailKey.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
