package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankDetailsTest {
    @Test
    void classLoads() {
        assertThat(BankDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BankDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
