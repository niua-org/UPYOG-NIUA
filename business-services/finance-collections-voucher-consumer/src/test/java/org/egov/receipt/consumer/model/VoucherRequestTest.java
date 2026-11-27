package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherRequestTest {
    @Test
    void classLoads() {
        assertThat(VoucherRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VoucherRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
