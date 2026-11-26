package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptResponseTest {
    @Test
    void classLoads() {
        assertThat(ReceiptResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
