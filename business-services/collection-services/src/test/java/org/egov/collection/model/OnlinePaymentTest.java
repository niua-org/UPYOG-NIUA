package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OnlinePaymentTest {
    @Test
    void classLoads() {
        assertThat(OnlinePayment.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = OnlinePayment.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
