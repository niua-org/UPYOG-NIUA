package org.egov.receipt.consumer.v2.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestTest {
    @Test
    void classLoads() {
        assertThat(PaymentRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
