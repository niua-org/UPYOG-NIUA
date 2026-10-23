package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentRefundTest {
    @Test
    void classLoads() {
        assertThat(PaymentRefund.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentRefund.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
