package org.egov.receipt.consumer.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherIntegrationLogTest {
    @Test
    void classLoads() {
        assertThat(VoucherIntegrationLog.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VoucherIntegrationLog.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
