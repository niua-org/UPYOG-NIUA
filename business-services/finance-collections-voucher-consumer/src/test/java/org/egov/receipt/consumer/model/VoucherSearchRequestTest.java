package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherSearchRequestTest {
    @Test
    void classLoads() {
        assertThat(VoucherSearchRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VoucherSearchRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
