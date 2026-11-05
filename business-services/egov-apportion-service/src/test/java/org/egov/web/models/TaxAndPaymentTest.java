package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxAndPaymentTest {
    @Test
    void classLoads() {
        assertThat(TaxAndPayment.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxAndPayment.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
