package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentWorkflowRequestTest {
    @Test
    void classLoads() {
        assertThat(PaymentWorkflowRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentWorkflowRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
