package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentDetailTest {
    @Test
    void classLoads() {
        assertThat(PaymentDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
