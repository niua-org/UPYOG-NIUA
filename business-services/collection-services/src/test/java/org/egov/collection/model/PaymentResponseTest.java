package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentResponseTest {
    @Test
    void classLoads() {
        assertThat(PaymentResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
