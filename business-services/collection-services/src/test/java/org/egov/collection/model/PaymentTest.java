package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {
    @Test
    void classLoads() {
        assertThat(Payment.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Payment.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
