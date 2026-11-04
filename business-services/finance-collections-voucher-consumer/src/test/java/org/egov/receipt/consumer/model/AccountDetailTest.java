package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountDetailTest {
    @Test
    void classLoads() {
        assertThat(AccountDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AccountDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
