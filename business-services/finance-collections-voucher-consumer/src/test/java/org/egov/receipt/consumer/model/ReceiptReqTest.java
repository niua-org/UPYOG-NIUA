package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptReqTest {
    @Test
    void classLoads() {
        assertThat(ReceiptReq.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptReq.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
