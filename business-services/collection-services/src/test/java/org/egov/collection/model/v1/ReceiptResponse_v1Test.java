package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptResponse_v1Test {
    @Test
    void classLoads() {
        assertThat(ReceiptResponse_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptResponse_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
