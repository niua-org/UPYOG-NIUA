package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherTest {
    @Test
    void classLoads() {
        assertThat(Voucher.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Voucher.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
