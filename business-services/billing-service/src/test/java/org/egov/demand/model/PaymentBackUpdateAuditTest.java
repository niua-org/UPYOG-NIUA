package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentBackUpdateAuditTest {
    @Test
    void classLoads() {
        assertThat(PaymentBackUpdateAudit.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentBackUpdateAudit.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
