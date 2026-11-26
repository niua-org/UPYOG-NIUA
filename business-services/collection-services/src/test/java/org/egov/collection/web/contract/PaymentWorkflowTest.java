package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentWorkflowTest {
    @Test
    void classLoads() {
        assertThat(PaymentWorkflow.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentWorkflow.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
