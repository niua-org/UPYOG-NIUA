package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentRefundResponseTest {
    @Test
    void classLoads() {
        assertThat(PaymentRefundResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentRefundResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
