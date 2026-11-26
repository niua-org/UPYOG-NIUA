package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherResponseTest {
    @Test
    void classLoads() {
        assertThat(VoucherResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VoucherResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
